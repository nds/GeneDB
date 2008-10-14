package org.genedb.querying.tmpquery;

import org.genedb.querying.core.LuceneQuery;
import org.genedb.querying.core.QueryClass;
import org.genedb.querying.core.QueryParam;

import org.apache.lucene.index.Term;
import org.apache.lucene.search.BooleanQuery;
import org.apache.lucene.search.TermQuery;
import org.apache.lucene.search.WildcardQuery;
import org.apache.lucene.search.BooleanClause.Occur;
import org.springframework.util.StringUtils;

import java.util.List;

@QueryClass(
        title="Coding and pseudogenes by protein length",
        shortDesc="Get a list of transcripts ",
        longDesc=""
    )
public class NameProductQuery extends LuceneQuery {

    @QueryParam(
            order=1,
            title="Minimum length of protein in bases"
    )
    private String search = "";


    @Override
    protected String getluceneIndexName() {
        return "org.gmod.schema.mapped.Feature";
    }

    @Override
    protected void getQueryTerms(List<org.apache.lucene.search.Query> queries) {

        BooleanQuery bq = new BooleanQuery();
        if(StringUtils.containsWhitespace(search)) {
            for(String term : search.split(" ")) {
                bq.add(new TermQuery(new Term("product",term.toLowerCase()
                    )), Occur.SHOULD);
            }
        } else {
            if (search.indexOf('*') == -1) {
                bq.add(new TermQuery(new Term("allNames",search.toLowerCase())), Occur.SHOULD);
               bq.add(new TermQuery(new Term("product",search.toLowerCase())), Occur.SHOULD);
            } else {
                bq.add(new WildcardQuery(new Term("allNames", search.toLowerCase())), Occur.SHOULD);
                bq.add(new WildcardQuery(new Term("product", search.toLowerCase())), Occur.SHOULD);
            }
        }

        queries.add(bq);
        queries.add(geneOrPseudogeneQuery);


//        BooleanQuery organismQuery = makeQueryForOrganisms(orgNames);
//        queries.add(organismQuery);
    }

    // ------ Autogenerated code below here

    public void setSearch(String search) {
        this.search = search;
    }

    public String getSearch() {
        return search;
    }

    @Override
    protected String[] getParamNames() {
        return new String[] {"search"};
    }


}
