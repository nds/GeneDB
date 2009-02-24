package org.genedb.querying.tmpquery;
  
import org.genedb.querying.core.QueryClass;
import org.genedb.querying.core.QueryParam;

import org.hibernate.validator.ClassValidator;
import org.hibernate.validator.InvalidValue;
import org.hibernate.validator.Min;
import org.springframework.validation.Errors;

@QueryClass(
        title="Coding and pseudogenes by protein length",
        shortDesc="Get a list of transcripts ",
        longDesc=""
    )
public class ProteinLengthQuery extends OrganismHqlQuery {
	
    @QueryParam(
            order=2,
            title="Minimum length of protein in bases"
    )
    @Min(value=1, message="{min.minimum}")
    private int min = 1;

    @QueryParam(
            order=3,
            title="Maximum length of protein in bases"
    )
    private int max = 500;

    @Override
    protected String getHql() {
        return "select f.uniqueName, f.organism.abbreviation from Feature f where f.type.name='polypeptide' and f.seqLen >= :min and f.seqLen <= :max @ORGANISM@ order by f.organism, f.uniqueName";
    }

    // ------ Autogenerated code below here
    
	public void setMin(int min) {
        this.min = min;
    }

    public void setMax(int max) {
        this.max = max;
    }

    public int getMin() {
        return min;
    }

    public int getMax() {
        return max;
    }

    @Override
    protected String[] getParamNames() {
    	String[] superParamNames = super.getParamNames();
    	String[] thisQuery = new String[] {"min", "max"};
    	return arrayAppend(superParamNames, thisQuery);
    }

	@Override
    protected void populateQueryWithParams(org.hibernate.Query query) {
    	super.populateQueryWithParams(query);
        query.setInteger("min", min);
        query.setInteger("max", max);
    }


	@Override
	public void validate(Object target, Errors errors) {
		ClassValidator<ProteinLengthQuery> lengthQueryValidator = new ClassValidator<ProteinLengthQuery>(ProteinLengthQuery.class);     
		ProteinLengthQuery query = (ProteinLengthQuery)target;
		InvalidValue[] invalids = lengthQueryValidator.getInvalidValues(query);
		for (InvalidValue invalidValue: invalids){
			errors.rejectValue(invalidValue.getPropertyPath(), null, invalidValue.getMessage());
		}
                
		//validate dependent properties
		if (!errors.hasErrors()) {
			int min = query.getMin();
			int max = query.getMax();
			if (min > max) {
				errors.reject("min.greater.than.max");
			}
		}
	}

	@Override
	@SuppressWarnings("unchecked")
	public boolean supports(Class clazz) {
		return ProteinLengthQuery.class.isAssignableFrom(clazz);
	}

}
