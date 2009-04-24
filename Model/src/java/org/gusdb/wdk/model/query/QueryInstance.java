/**
 * 
 */
package org.gusdb.wdk.model.query;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.AnswerParam;
import org.gusdb.wdk.model.Param;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.CacheFactory;
import org.gusdb.wdk.model.dbms.QueryInfo;
import org.gusdb.wdk.model.dbms.ResultFactory;
import org.gusdb.wdk.model.dbms.ResultList;
import org.gusdb.wdk.model.dbms.SqlUtils;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author Jerric Gao
 * 
 */
public abstract class QueryInstance {

    public abstract void createCache(Connection connection, String tableName,
            int instanceId) throws NoSuchAlgorithmException, WdkModelException,
            SQLException, JSONException, WdkUserException;

    public abstract void insertToCache(Connection connection, String tableName,
            int instanceId) throws WdkModelException, SQLException,
            NoSuchAlgorithmException, JSONException, WdkUserException;

    public abstract String getSql() throws WdkModelException, SQLException,
            NoSuchAlgorithmException, JSONException, WdkUserException;

    protected abstract void appendSJONContent(JSONObject jsInstance)
            throws JSONException;

    protected abstract ResultList getUncachedResults()
            throws WdkModelException, SQLException, NoSuchAlgorithmException,
            JSONException, WdkUserException;

    private static final Logger logger = Logger.getLogger(QueryInstance.class);

    private Integer instanceId;
    protected Query query;
    protected WdkModel wdkModel;
    protected Map<String, Object> values;
    protected String resultMessage;
    protected boolean cached;

    private String checksum;
    private Integer resultSize;

    protected QueryInstance(Query query, Map<String, Object> values)
            throws WdkModelException {
        this.query = query;
        this.wdkModel = query.getWdkModel();
        this.cached = query.isCached();

        logger.debug("validating param values of query [" + query.getFullName()
                + "]");
        setValues(values);
    }

    public Query getQuery() {
        return query;
    }

    /**
     * @return the instanceId
     * @throws JSONException
     * @throws WdkModelException
     * @throws SQLException
     * @throws NoSuchAlgorithmException
     * @throws WdkUserException
     */
    public Integer getInstanceId() throws NoSuchAlgorithmException,
            SQLException, WdkModelException, JSONException, WdkUserException {
        if (instanceId == null) {
            ResultFactory resultFactory = wdkModel.getResultFactory();
            String[] indexColumns = query.getIndexColumns();
            instanceId = resultFactory.getInstanceId(this, indexColumns);
        }
        return instanceId;
    }

    /**
     * @param instanceId
     *            the instanceId to set
     */
    public void setInstanceId(Integer instanceId) {
        this.instanceId = instanceId;
    }

    private void setValues(Map<String, Object> values) throws WdkModelException {
        values = new LinkedHashMap<String, Object>(values);
        // apply empty values, and validate them on assignment
        query.validateValues(values);
        // passed, assign the value
        this.values = values;
        checksum = null;
    }

    public String getResultMessage() {
        return resultMessage;
    }

    public void setResultMessage(String message) {
        this.resultMessage = message;
    }

    /**
     * @return the cached
     */
    public boolean isCached() {
        return this.cached;
    }

    /**
     * @param cached
     *            the cached to set
     */
    public void setCached(boolean cached) {
        this.cached = cached;
    }

    public String getChecksum() throws NoSuchAlgorithmException,
            WdkModelException, JSONException, SQLException, WdkUserException {
        if (checksum == null) {
            JSONObject jsQuery = getJSONContent();
            checksum = Utilities.encrypt(jsQuery.toString());
        }
        return checksum;
    }

    public JSONObject getJSONContent() throws JSONException,
            NoSuchAlgorithmException, WdkModelException, SQLException, WdkUserException {
        JSONObject jsInstance = new JSONObject();
        jsInstance.put("query", query.getFullName());
        jsInstance.put("querySignature", query.getChecksum());

        jsInstance.put("params", getIndependentParamJSONObject());

        // include extra info from child
        appendSJONContent(jsInstance);

        return jsInstance;
    }

    public JSONObject getDependentParamJSONObject() throws JSONException,
            NoSuchAlgorithmException, WdkModelException, SQLException,
            WdkUserException {
        // construct param-value map; param is sorted by name
        String[] paramNames = new String[values.size()];
        values.keySet().toArray(paramNames);
        Arrays.sort(paramNames);

        JSONObject jsParams = new JSONObject();
        for (String paramName : paramNames) {
            Object paramValue = values.get(paramName);
            jsParams.put(paramName, paramValue);
        }
        return jsParams;
    }

    public JSONObject getIndependentParamJSONObject() throws JSONException,
            NoSuchAlgorithmException, WdkModelException, SQLException,
            WdkUserException {
        // construct param-value map; param is sorted by name
        String[] paramNames = new String[values.size()];
        values.keySet().toArray(paramNames);
        Arrays.sort(paramNames);

        Map<String, Param> params = query.getParamMap();
        JSONObject jsParams = new JSONObject();
        for (String paramName : paramNames) {
            Object dependentValue = values.get(paramName);
            Param param = params.get(paramName);
            Object independentValue = param.dependentValueToIndependentValue(dependentValue);
            jsParams.put(paramName, independentValue);
        }
        return jsParams;
    }

    public ResultList getResults() throws NoSuchAlgorithmException,
            SQLException, WdkModelException, JSONException, WdkUserException {
        logger.debug("retrieving results of query [" + query.getFullName()
                + "]");

        ResultList resultList = (cached) ? getCachedResults()
                : getUncachedResults();

        logger.debug("results of query [" + query.getFullName()
                + "] retrieved.");

        return resultList;
    }

    public int getResultSize() throws NoSuchAlgorithmException, SQLException,
            WdkModelException, JSONException, WdkUserException {
        logger.debug("start getting query size");
        if (resultSize == null) {
            StringBuffer sql = new StringBuffer("SELECT count(*) FROM (");
            sql.append(getSql()).append(") f");
            DataSource dataSource = wdkModel.getQueryPlatform().getDataSource();
            Object objSize = SqlUtils.executeScalar(dataSource, sql.toString());
            resultSize = Integer.parseInt(objSize.toString());
        }
        logger.debug("end getting query size");
        return resultSize;
    }

    public Map<String, Object> getValues() {
        return values;
    }

    private ResultList getCachedResults() throws NoSuchAlgorithmException,
            SQLException, WdkModelException, JSONException, WdkUserException {
        ResultFactory factory = wdkModel.getResultFactory();
        return factory.getCachedResults(this);
    }

    protected Map<String, String> getInternalParamValues()
            throws WdkModelException, SQLException, NoSuchAlgorithmException,
            JSONException, WdkUserException {
        Map<String, String> internalValues = new LinkedHashMap<String, String>();
        Map<String, Param> params = query.getParamMap();
        for (String paramName : params.keySet()) {
            Param param = params.get(paramName);
            String defaultValue = param.getDefault();
            if (param.isAllowEmpty()) defaultValue = param.getEmptyValue();
            Object externalValue = values.containsKey(paramName) ? values.get(paramName)
                    : defaultValue;

            String internalValue = param.getInternalValue(externalValue);
            if (internalValue == null)
                throw new WdkModelException("The internal value of param '"
                        + paramName + "' with given term '" + externalValue
                        + "' cannot be found.");

            internalValues.put(paramName, internalValue);
        }
        return internalValues;
    }

    public String getCachedSql() throws NoSuchAlgorithmException, SQLException,
            WdkModelException, JSONException, WdkUserException {
        CacheFactory cacheFactory = wdkModel.getResultFactory().getCacheFactory();
        QueryInfo queryInfo = cacheFactory.getQueryInfo(getQuery());

        String cacheTable = queryInfo.getCacheTable();
        int instanceId = getInstanceId();

        StringBuffer sql = new StringBuffer("SELECT * FROM ");
        sql.append(cacheTable).append(" WHERE ");
        sql.append(CacheFactory.COLUMN_INSTANCE_ID);
        sql.append(" = ").append(instanceId);
        return sql.toString();
    }
}
