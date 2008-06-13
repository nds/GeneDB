/*
 * Copyright (c) 2006 Genome Research Limited.
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU Library General Public License as published
 * by  the Free Software Foundation; either version 2 of the License or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful, but
 * WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU Library General Public License for more details.
 *
 * You should have received a copy of the GNU Library General Public License
 * along with this program; see the file COPYING.LIB.  If not, write to
 * the Free Software Foundation Inc., 59 Temple Place - Suite 330,
 * Boston, MA  02111-1307 USA
 */

package org.genedb.web.mvc.controller;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.lucene.document.Document;
import org.apache.lucene.index.IndexReader;
import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanClause;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.Hits;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.genedb.db.dao.SequenceDao;
import org.genedb.db.taxon.TaxonNode;
import org.gmod.schema.sequence.Feature;
import org.springframework.util.StringUtils;
import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

/**
 * Looks up a feature by uniquename, and possibly synonyms
 *
 * @author Chinmay Patel (cp2)
 * @author Adrian Tivey (art)
 */
public class NamedFeatureController extends TaxonNodeBindingFormController {

    //private static final Logger logger = Logger.getLogger(NamedFeatureController.class);

    private String listResultsView;
    private SequenceDao sequenceDao;
    private LuceneDao luceneDao;
    private String geneView, geneDetailsView;

    private static final BooleanQuery geneOrPseudogeneQuery = new BooleanQuery();
    static {
        geneOrPseudogeneQuery.add(new TermQuery(new Term("cvTerm.name","gene")), Occur.SHOULD);
        geneOrPseudogeneQuery.add(new TermQuery(new Term("cvTerm.name","pseudogene")), Occur.SHOULD);
    }

    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response,
            Object command, BindException be) throws Exception {

        NameLookupBean nlb = (NameLookupBean) command;
        String name = nlb.getName();
        TaxonNode[] taxonNodes = nlb.getOrganism();
        Map<String, Object> model = new HashMap<String, Object>(2);
        String viewName = listResultsView;
        List<ResultHit> results = new ArrayList<ResultHit>();

        IndexReader ir = luceneDao.openIndex("org.gmod.schema.sequence.Feature");

        BooleanQuery geneNameQuery = new BooleanQuery();
        
        if(StringUtils.containsWhitespace(name)) {
            for(String term : name.split(" ")) {
                geneNameQuery.add(new TermQuery(new Term("product",term.toLowerCase()
                    )), Occur.SHOULD);
            }
        } else {
            if (name.indexOf('*') == -1) {
                geneNameQuery.add(new TermQuery(new Term("allNames",name.toLowerCase())), Occur.SHOULD);
                geneNameQuery.add(new TermQuery(new Term("product",name.toLowerCase())), Occur.SHOULD);
            } else {
                geneNameQuery.add(new WildcardQuery(new Term("allNames", name.toLowerCase())), Occur.SHOULD);
                geneNameQuery.add(new WildcardQuery(new Term("product", name.toLowerCase())), Occur.SHOULD);
            }
        }
        
        
        
        BooleanQuery booleanQuery = new BooleanQuery();
        booleanQuery.add(new BooleanClause(geneOrPseudogeneQuery, Occur.MUST));
        booleanQuery.add(new BooleanClause(geneNameQuery, Occur.MUST));
        
        
        if(taxonNodes != null) {
            List<String> orgNames = new ArrayList<String>();
            for (TaxonNode node : taxonNodes) {
                orgNames.addAll(node.getAllChildrenNames());
            }
            BooleanQuery organismQuery = new BooleanQuery();
            for (String organism : orgNames) {
                organismQuery.add(new TermQuery(new Term("organism.commonName",organism)), Occur.SHOULD);
            }
            booleanQuery.add(new BooleanClause(organismQuery,Occur.MUST));
        }
        
        logger.debug(String.format("Lucene query is '%s'", booleanQuery.toString()));
        Hits hits = luceneDao.search(ir, booleanQuery);
        
        switch (hits.length()) {
        case 0: {
            // Temporary check as the Lucene index isn't automatically
            // up-to-date
            Feature feature = sequenceDao.getFeatureByUniqueName(name, Feature.class);
            if (feature != null) {
                logger.info(String.format("Lucene did not find feature '%s'; we found it in the database", name));
                model = GeneDBWebUtils.prepareFeature(feature, model);
                viewName = geneView;
                break;
            }
            logger.warn(String.format("Failed to find feature '%s'", name));
            be.reject("no.results");
            return showForm(request, response, be);
        }
        case 1: {
            Document doc = hits.doc(0);
            logger.debug(String.format("Lucene found feature '%s'", doc.get("uniqueName")));
            if ("gene".equals(doc.get("cvTerm.name")) || "pseudogene".equals(doc.get("cvTerm.name"))) {
                GeneDBWebUtils.prepareGene(doc.get("uniqueName"), model);
            } else {
                GeneDBWebUtils.prepareTranscript(doc.get("uniqueName"), model);
            }
            viewName = geneView;
            break;
        }
        default:
            for (int i = 0; i < hits.length(); i++) {
                Document doc = hits.doc(i);
                ResultHit rh = new ResultHit();
                rh.setName(doc.get("uniqueName"));
                rh.setType("gene");
                rh.setProduct(doc.get("product"));
                rh.setOrganism(doc.get("organism.commonName"));
                results.add(rh);
            }
            viewName = listResultsView;
            model.put("luceneResults", results);
        }

        if (nlb.isHistory()) {
            List<String> ids = new ArrayList<String>(results.size());
            for (ResultHit feature : results) {
                ids.add(feature.getName());
            }
            HistoryManager historyManager = getHistoryManagerFactory().getHistoryManager(
                request.getSession());
            historyManager.addHistoryItems("name lookup '" + nlb + "'", ids);
        }

        if (geneView.equals(viewName) && nlb.isDetailsOnly())
            viewName = geneDetailsView;
        return new ModelAndView(viewName, model);
    }

    public void setLuceneDao(LuceneDao luceneDao) {
        this.luceneDao = luceneDao;
    }

    public void setListResultsView(String listResultsView) {
        this.listResultsView = listResultsView;
    }

    public void setSequenceDao(SequenceDao sequenceDao) {
        this.sequenceDao = sequenceDao;
    }

    public void setGeneView(String geneView) {
        this.geneView = geneView;
    }

    public void setGeneDetailsView(String geneDetailsView) {
        this.geneDetailsView = geneDetailsView;
    }
}

class NameLookupBean {

    private String name; // The name to lookup, using * for wildcards
    private boolean addWildcard = false;
    private String featureType = "gene";
    private boolean useProduct = false;
    private boolean history = false;
    private boolean detailsOnly = false;
    private TaxonNode[] organism;

    public void setName(String name) {
        this.name = name;
    }

    public String getName() {
        if (addWildcard) {
            StringBuilder ret = new StringBuilder(this.name);
            if (!(ret.charAt(0) == '*')) {
                ret.insert(0, '*');
            }
            if (!(ret.charAt(ret.length() - 1) == '*')) {
                ret.append('*');
            }
            return ret.toString();
        }
        return this.name;
    }

    public String getFeatureType() {
        return featureType;
    }

    public void setFeatureType(String featureType) {
        this.featureType = featureType;
    }

    public void setAddWildcard(boolean addWildcard) {
        this.addWildcard = addWildcard;
    }

    public boolean isUseProduct() {
        return useProduct;
    }

    public void setUseProduct(boolean useProduct) {
        this.useProduct = useProduct;
    }

    public boolean isHistory() {
        return history;
    }

    public void setHistory(boolean history) {
        this.history = history;
    }

    public boolean isDetailsOnly() {
        return detailsOnly;
    }

    public void setDetailsOnly(boolean detailsOnly) {
        this.detailsOnly = detailsOnly;
    }

    public TaxonNode[] getOrganism() {
        return organism;
    }

    public void setOrganism(TaxonNode[] organism) {
        this.organism = organism;
    }

}
