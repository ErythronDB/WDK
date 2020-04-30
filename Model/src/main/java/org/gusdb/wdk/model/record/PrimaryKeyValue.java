package org.gusdb.wdk.model.record;

import static java.util.Arrays.asList;
import static org.gusdb.fgputil.FormatUtil.join;
import static org.gusdb.fgputil.functional.Functions.mapToList;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.gusdb.fgputil.FormatUtil;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;

public class PrimaryKeyValue {

  private final PrimaryKeyDefinition _pkDef;
  private final Map<String, Object> _pkValues;

  public PrimaryKeyValue(PrimaryKeyDefinition pkDef, Map<String, ? extends Object> pkValues) throws WdkModelException {
    _pkDef = pkDef;
    // make sure incoming values match columns in definition
    _pkValues = parseValues(pkDef, pkValues);
  }

  private static Map<String, Object> parseValues(PrimaryKeyDefinition pkDef, Map<String, ? extends Object> pkValues)
      throws WdkModelException {
    Function<String, WdkModelException> error = message -> new WdkModelException(message +
        " Expected: " + FormatUtil.arrayToString(pkDef.getColumnRefs()) +
        ", Received: " + FormatUtil.prettyPrint(pkValues));

    if (pkDef.getColumnRefs().length != pkValues.size()) {
      throw error.apply("More PK values passed than expected.");
    }
    else {
      Map<String, Object> copyOfPassedValues = new HashMap<>(pkValues);
      Map<String, Object> parsedValues = new LinkedHashMap<>();
      for (String expectedCol : pkDef.getColumnRefs()) {
        if (!copyOfPassedValues.containsKey(expectedCol)) {
          throw error.apply("Expected PK value '" + expectedCol + "' not present in passed values.");
        }
        parsedValues.put(expectedCol, copyOfPassedValues.get(expectedCol));
        copyOfPassedValues.remove(expectedCol);
      }
      if (copyOfPassedValues.isEmpty()) {
        // all values found and no extras
        return parsedValues;
      }
      else {
        return parsedValues;
        //throw error.apply("Unexpected PK value(s) passed: " +
        //    copyOfPassedValues.keySet().stream().collect(Collectors.joining(", ")));
      }
    }
  }

  public PrimaryKeyDefinition getPrimaryKeyDefinition() {
    return _pkDef;
  }

  public Map<String, Object> getRawValues() {
    return new LinkedHashMap<String, Object>(_pkValues);
  }

  public Map<String, String> getValues() {
    Map<String, String> values = new LinkedHashMap<>();
    for (String column : _pkValues.keySet()) {
      String copy = Utilities.parseValue(_pkValues.get(column));
      values.put(column, copy);
    }
    return values;
  }

  public String getValuesAsString() {
    return join(mapToList(_pkValues.entrySet(), entry -> entry.getKey() + " = " + entry.getValue()), ", ");
  }

  public static boolean rawValuesDiffer(Map<String, ? extends Object> map1, Map<String, ? extends Object> map2) {
    if (map1.size() != map2.size()) return true;
    for (String key : map1.keySet()) {
      if (!map2.containsKey(key)) return true;
      if (!map2.get(key).equals(map1.get(key))) return true;
    }
    return false;
  }

  @Override
  public boolean equals(Object obj) {
    return (obj instanceof PrimaryKeyValue) &&
        !rawValuesDiffer(_pkValues, ((PrimaryKeyValue)obj)._pkValues);
  }

  @Override
  public int hashCode() {
    List<String> orderedKeys = new ArrayList<String>(_pkValues.keySet());
    Collections.sort(orderedKeys);
    int hashCode = 0;
    for (String pkKey : orderedKeys) {
      Object pkValue = _pkValues.get(pkKey);
      if (pkValue != null) {
        hashCode ^= pkValue.toString().hashCode();
      }
    }
    return hashCode;
  }

  @Override
  public String toString() {
    return FormatUtil.join(getValues().values().toArray(), "/");
  }

  public static List<String[]> toStringArrays(
      Collection<PrimaryKeyValue> recordPks) {
    return mapToList(recordPks, pkValue ->
        mapToList(asList(pkValue._pkDef.getColumnRefs()),
            colName -> pkValue.getValues().get(colName)
        ).toArray(new String[pkValue._pkDef.getColumnRefs().length])
    );
  }
}
