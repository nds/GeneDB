package org.genedb.db.fixup;

import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Map;

/**
 * Caches the cvterms used to type features and organismprops,
 * and makes it easy to retrieve the ID from the name or vice versa.
 * Used by {@link FixResidues}.
 *
 * @author rh11
 *
 */
public class TypeCodes {
    private Map<String,Map<String,Integer>> idByTermNameByCvName = new HashMap<String,Map<String,Integer>>();
    private Map<Integer,String> cvNameById = new HashMap<Integer,String>();
    private Map<Integer,String> termNameById = new HashMap<Integer,String>();

    public TypeCodes(Connection conn) throws SQLException {
        PreparedStatement st = conn.prepareStatement(
            "select cvterm_id"
            +"      , cv.name as cv_name"
            +"      , cvterm.name as cvterm_name"
            +" from cvterm"
            +" join cv using (cv_id)"
            +" join ("
            +"    select distinct type_id as cvterm_id from feature"
            +"    union"
            +"    select distinct type_id as cvterm_id from organismprop"
            +"    union"
            +"    select distinct type_id as cvterm_id from featureprop"
            +") feature_types using (cvterm_id)"
        );
        try {
            ResultSet rs = st.executeQuery();
            while (rs.next()) {
                int cvTermId = rs.getInt("cvterm_id");
                String cvName = rs.getString("cv_name");
                String cvTermName = rs.getString("cvterm_name");

                if (!idByTermNameByCvName.containsKey(cvName))
                    idByTermNameByCvName.put(cvName, new HashMap<String,Integer>());
                idByTermNameByCvName.get(cvName).put(cvTermName, cvTermId);

                cvNameById.put(cvTermId, cvName);
                termNameById.put(cvTermId, cvTermName);
            }
        }
        finally {
            try {
                st.close();
            }
            catch (SQLException e) {
                System.err.println("SQLException from close(), probably caused by a previous exception");
                e.printStackTrace(System.err);
            }
        }
    }

    public int typeId(String cvName, String cvTermName) {
        Map<String,Integer> termMap = idByTermNameByCvName.get(cvName);
        if (termMap == null)
            throw new IllegalArgumentException(String.format("CV '%s' not found", cvName));
        Integer result = termMap.get(cvTermName);
        if (result == null)
            throw new IllegalArgumentException(String.format("Term '%s' not found in CV '%s'", cvTermName, cvName));
        return result;
    }

    public String cvName(int cvTermId) {
        if (!cvNameById.containsKey(cvTermId))
            throw new IllegalArgumentException(String.format("CV term ID=%d not found", cvTermId));
        return cvNameById.get(cvTermId);
    }
    public String termName(int cvTermId) {
        if (!termNameById.containsKey(cvTermId))
            throw new IllegalArgumentException(String.format("CV term ID=%d not found", cvTermId));
        return termNameById.get(cvTermId);
    }
}
