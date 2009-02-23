package org.genedb.querying.tmpquery;

import java.util.HashSet;
import java.util.Set;

import org.genedb.db.taxon.TaxonNode;
import org.genedb.querying.core.HqlQuery;    
import org.genedb.querying.core.QueryClass;
import org.genedb.querying.core.QueryParam;

import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;
import org.hibernate.validator.Max;
import org.hibernate.validator.Min;

import org.springframework.util.StringUtils;
import org.springframework.validation.Errors;
import org.springframework.validation.Validator;

@QueryClass(
        title="Coding and pseudogenes by protein length",
        shortDesc="Get a list of transcripts ",
        longDesc=""
    )
public abstract class OrganismHqlQuery extends HqlQuery {

    @QueryParam(
            order=1,
            title="Organism(s) to search"
    )
    protected TaxonNode[] taxons;	

    
    @Override
	protected String getOrganismHql() {
    	if (taxons==null || taxons.length==0) {
    		return null;
    	}
		return "and f.organism.abbreviation in (:organismList)";
	}
    

    public void setTaxons(TaxonNode[] taxons) {
		this.taxons = taxons;
	}

	public TaxonNode[] getTaxons() {
		return taxons;
	}

    @Override
    protected String[] getParamNames() {
    	return new String[] {"taxons"};
    }

    @Override
    protected void populateQueryWithParams(org.hibernate.Query query) {
    	if (taxons != null && taxons.length > 0) {
    		Set<String> names = new HashSet<String>();
    		for (TaxonNode node : taxons) {
				names.addAll(node.getAllChildrenNames());
			}
    		query.setParameterList("organismList", names);
    		System.err.println(StringUtils.collectionToDelimitedString(names, " "));
    	}
    }

    protected String[] arrayAppend(String[] superParamNames, String[] thisQuery) {
		String[] ret = new String[superParamNames.length+thisQuery.length];
		System.arraycopy(superParamNames, 0, ret, 0, superParamNames.length);
		System.arraycopy(thisQuery, 0, ret, superParamNames.length, thisQuery.length);
		return ret;
	}

//    public Validator getValidator() {
//        return new Validator() {
//            @Override
//            public void validate(Object target, Errors errors) {
//            	ClassValidator<OrganismHqlQuery> lengthQueryValidator = new ClassValidator<OrganismHqlQuery>(OrganismHqlQuery.class);     
//            	OrganismHqlQuery query = (OrganismHqlQuery)target;
//            	InvalidValue[] invalids = lengthQueryValidator.getInvalidValues(query);
//            	for (InvalidValue invalidValue: invalids){
//            		errors.rejectValue(invalidValue.getPropertyPath(), null, invalidValue.getMessage());
//            	}
//                
//                //validate dependent properties
//            	if (!errors.hasErrors()) {
//            		int min = query.getMin();
//            		int max = query.getMax();
//            		if (min > max) {
//            			errors.reject("min.greater.than.max");
//            		}
//                }
//            }
//
//            @Override
//            @SuppressWarnings("unchecked")
//            public boolean supports(Class clazz) {
//                return OrganismHqlQuery.class.isAssignableFrom(clazz);
//            }
//        };
//    }

}
