package org.gusdb.wdk.model.query;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.Map;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.AnswerFilterInstance;
import org.gusdb.wdk.model.BooleanOperator;
import org.gusdb.wdk.model.RecordClass;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.StringParam;
import org.gusdb.wdk.model.user.User;
import org.json.JSONException;

/**
 * BooleanQueryInstance.java
 * 
 * Instance instantiated by a BooleanQuery. Takes Answers as values for its
 * parameters along with a boolean operation, and returns a result.
 * 
 * Created: Wed May 19 15:11:30 2004
 * 
 * @author David Barkan
 * @version $Revision$ $Date: 2005-08-09 22:24:36 -0400 (Tue, 09 Aug
 *          2005) $ $Author$
 */

public class BooleanQueryInstance extends SqlQueryInstance {

    private static final Logger logger = Logger.getLogger(BooleanQueryInstance.class);

    private BooleanQuery booleanQuery;

    /**
     * @param query
     * @param values
     * @throws WdkModelException
     * @throws WdkUserException
     * @throws JSONException
     * @throws SQLException
     * @throws NoSuchAlgorithmException
     * @throws SQLException
     * @throws JSONException
     * @throws WdkUserException
     * @throws NoSuchAlgorithmException
     */
    protected BooleanQueryInstance(User user, BooleanQuery query,
            Map<String, String> values) throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        super(user, query, values);
        this.booleanQuery = query;
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.query.SqlQueryInstance#getUncachedSql()
     */
    @Override
    public String getUncachedSql() throws WdkModelException, SQLException,
            NoSuchAlgorithmException, JSONException, WdkUserException {

        // needs to apply the view to each operand before boolean
        StringBuffer sql = new StringBuffer();

        // get the use_boolean filter param
        boolean booleanFlag = isUseBooleanFilter();

        logger.info("Boolean expansion flag: " + booleanFlag);

        Map<String, String> InternalValues = getInternalParamValues();

        // construct the filter query for the first child
        AnswerParam leftParam = booleanQuery.getLeftOperandParam();
        String leftSubSql = (String) InternalValues.get(leftParam.getName());
        String leftSql = constructOperandSql(leftParam, leftSubSql, booleanFlag);

        AnswerParam rightParam = booleanQuery.getRightOperandParam();
        String rightSubSql = (String) InternalValues.get(rightParam.getName());
        String rightSql = constructOperandSql(rightParam, rightSubSql,
                booleanFlag);

        String operator = InternalValues.get(booleanQuery.getOperatorParam().getName());
        BooleanOperator op = BooleanOperator.parse(operator);
        DBPlatform platform = wdkModel.getQueryPlatform();
        operator = op.getOperator(platform);

        if (op == BooleanOperator.RIGHT_MINUS) {
            sql.append("(").append(rightSql).append(") ");
            sql.append(operator);
            sql.append(" (").append(leftSql).append(")");
        } else {
            sql.append("(").append(leftSql).append(")");
            sql.append(operator);
            sql.append("(").append(rightSql).append(")");
        }

        return sql.toString();
    }

    private String constructOperandSql(AnswerParam stepParam, String subSql,
            boolean booleanFlag) throws NoSuchAlgorithmException,
            WdkModelException, SQLException, JSONException, WdkUserException {
        RecordClass recordClass = booleanQuery.getRecordClass();

        // create a template sql, and use answerParam to do the replacement
        String innerSql = "$$" + stepParam.getName() + "$$";
        innerSql = stepParam.replaceSql(innerSql, subSql);

        // apply the filter query if needed
        AnswerFilterInstance filter = recordClass.getBooleanExpansionFilter();
        if (booleanFlag && filter != null) {
            innerSql = filter.applyFilter(user, innerSql);
        }

        // limit the column output
        StringBuffer sql = new StringBuffer("SELECT ");

        // put columns in
        boolean firstColumn = true;
        for (Column column : booleanQuery.getColumns()) {
            if (firstColumn) firstColumn = false;
            else sql.append(", ");
            sql.append(column.getName());
        }
        sql.append(" FROM (").append(innerSql).append(") f");
        return sql.toString();
    }

    public boolean isUseBooleanFilter() {
        StringParam useBooleanFilter = booleanQuery.getUseBooleanFilter();
        String strBooleanFlag = (String) values.get(useBooleanFilter.getName());
        return Boolean.parseBoolean(strBooleanFlag);
    }
}