package org.gusdb.wdk.model.query.param;

import java.util.LinkedHashMap;
import java.util.Map;

import org.gusdb.fgputil.cache.ItemFetcher;
import org.gusdb.fgputil.cache.UnfetchableItemException;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.ResultList;
import org.gusdb.wdk.model.query.Query;
import org.gusdb.wdk.model.query.QueryInstance;
import org.gusdb.wdk.model.user.User;
import org.json.JSONException;
import org.json.JSONObject;

public class MetaDataSpecItemFetcher implements ItemFetcher<String, Map<String, Map<String, String>>> {
  private Query query;
  private Map<String, String> paramValues;
  private User user;
  
  public MetaDataSpecItemFetcher(Query metaDataQuery, Map<String, String> paramValues, User user) {
    this.query = metaDataQuery;
    this.paramValues = paramValues;
    this.user = user;
  }
  
  public Map<String, Map<String, String>> fetchItem(String cacheKey) throws UnfetchableItemException {

    try {
      QueryInstance<?> instance = query.makeInstance(user, paramValues, true, 0, paramValues);
      Map<String, Map<String, String>> metadata = new LinkedHashMap<>();
      ResultList resultList = instance.getResults();
      try {
        while (resultList.next()) {
          String property = (String) resultList.get(FilterParam.COLUMN_PROPERTY);
          String info = (String) resultList.get(FilterParam.COLUMN_SPEC_PROPERTY);
          String data = (String) resultList.get(FilterParam.COLUMN_SPEC_VALUE);
          Map<String, String> propertyMeta = metadata.get(property);
          if (propertyMeta == null) {
            propertyMeta = new LinkedHashMap<>();
            metadata.put(property, propertyMeta);
          }
          propertyMeta.put(info, data);
        }
      }
      finally {
        resultList.close();
      }
      return metadata;
    }
    catch (WdkModelException | WdkUserException ex) {
      throw new UnfetchableItemException(ex);
    }
  }
  
  public Map<String, Map<String, String>> updateItem(String key, Map<String, Map<String, String>> item) {
    return null;
  }

  public String getCacheKey() throws WdkModelException, JSONException {
   JSONObject cacheKeyJson = new JSONObject();
    cacheKeyJson.put("queryName", query.getName());
    JSONObject paramValuesJson = new JSONObject();
    for (String paramName : paramValues.keySet()) 
      if (query.getParamMap() != null && query.getParamMap().containsKey(paramName)) paramValuesJson.put(paramName, paramValues.get(paramName));
    cacheKeyJson.put("paramValues", paramValuesJson);
    return cacheKeyJson.toString();
  }
  
  public boolean itemNeedsUpdating(Map<String, Map<String, String>> item) {
    return false;
   }
   

}
