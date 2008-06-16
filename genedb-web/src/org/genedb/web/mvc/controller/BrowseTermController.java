/*
 * Copyright (c) 2006-2007 Genome Research Limited.
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

import org.apache.log4j.Logger;
import org.genedb.db.dao.SequenceDao;
import org.genedb.db.taxon.TaxonNode;

import org.gmod.schema.sequence.Feature;

import org.springframework.validation.BindException;
import org.springframework.web.servlet.ModelAndView;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;




/**
 * Looks up a feature by uniquename, and possibly synonyms
 * 
 * @author Chinmay Patel (cp2)
 * @author Adrian Tivey (art)
 */
public class BrowseTermController extends TaxonNodeBindingFormController {
	
	private static final Logger logger = Logger.getLogger(BrowseTermController.class);
	private SequenceDao sequenceDao;

    @SuppressWarnings("unchecked")
	@Override
	protected Map referenceData(HttpServletRequest request) throws Exception {
    	Map reference = new HashMap();
    	reference.put("categories", BrowseCategory.values());
    	return reference;
	}
	
    @Override
    protected ModelAndView onSubmit(HttpServletRequest request, HttpServletResponse response, Object command, BindException be) throws Exception {

        BrowseTermBean btb = (BrowseTermBean) command;
        String orgNames = TaxonUtils.getOrgNamesInHqlFormat(btb.getOrg());
        
        List<Feature> results = sequenceDao.getFeaturesByCvNameAndCvTermNameAndOrganisms(btb.getCategory().toString(), btb.getTerm(), orgNames);
        
        if (results == null || results.size() == 0) {
            logger.info("result is null"); // TODO Improve text
            be.reject("no.results");
            return showForm(request, response, be);
        }
        
        ModelAndView mav = new ModelAndView(getSuccessView());
        
        List<Feature> newResults = new ArrayList<Feature>(results.size());
        for (Feature feature : results) {
			if (!GeneUtils.isPartOfGene(feature)) {
				newResults.add(feature);
			} else {
				logger.info("Transforming '"+feature.getUniqueName()+"' as not part of gene");
				newResults.add(GeneUtils.getGeneFromPart(feature));
			}
		}

        mav.addObject("results", newResults);
        mav.addObject("controllerPath", "/BrowseTerm");

        return mav;
    }

	public void setSequenceDao(SequenceDao sequenceDao) {
		this.sequenceDao = sequenceDao;
	}

}


class BrowseTermBean {
    
    private TaxonNode[] organism;
    private BrowseCategory category;
    private String term;
    private String org;
    
    public BrowseCategory getCategory() {
        return this.category;
    }
    public void setCategory(BrowseCategory category) {
        this.category = category;
    }
    public TaxonNode[] getOrganism() {
        return this.organism;
    }
    public void setOrganism(TaxonNode[] organism) {
        this.organism = organism;
    }
    public String getTerm() {
        return this.term;
    }
    public void setTerm(String term) {
        this.term = term;
    }
	public String getOrg() {
		return org;
	}
	public void setOrg(String org) {
		this.org = org;
	}

}

