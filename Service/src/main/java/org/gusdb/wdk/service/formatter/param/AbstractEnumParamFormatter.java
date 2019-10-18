package org.gusdb.wdk.service.formatter.param;

import java.util.Arrays;
import java.util.List;

import org.gusdb.fgputil.validation.ValidObjectFactory.DisplayablyValid;
import org.gusdb.wdk.core.api.JsonKeys;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.query.param.AbstractEnumParam;
import org.gusdb.wdk.model.query.param.EnumParamTermNode;
import org.gusdb.wdk.model.query.param.EnumParamVocabInstance;
import org.gusdb.wdk.model.query.spec.ParameterContainerInstanceSpec;
import org.json.JSONArray;
import org.json.JSONObject;

public abstract class AbstractEnumParamFormatter extends ParamFormatter<AbstractEnumParam> {

  private static final String DUMMY_VALUE = "@@fake@@";

  AbstractEnumParamFormatter(AbstractEnumParam param) {
    super(param);
  }

  @Override
  public <S extends ParameterContainerInstanceSpec<S>> JSONObject getJson(DisplayablyValid<S> spec) throws WdkModelException {
    return getBaseJson(spec)
        .put(JsonKeys.COUNT_ONLY_LEAVES, _param.getCountOnlyLeaves())
        .put(JsonKeys.MAX_SELECTED_COUNT, _param.getMaxSelectedCount())
        .put(JsonKeys.MIN_SELECTED_COUNT, _param.getMinSelectedCount())
        .put(JsonKeys.IS_MULTIPICK, _param.getMultiPick())
        .put(JsonKeys.DISPLAY_TYPE, _param.getDisplayType())
        .put(JsonKeys.DEPTH_EXPANDED, _param.getDepthExpanded());
  }

  protected JSONArray getVocabArrayJson(EnumParamVocabInstance vocabInstance) throws WdkModelException {
    List<List<String>> vocabRows = vocabInstance.getFullVocab();
    JSONArray jsonRows = new JSONArray();
    for (List<String> row : vocabRows) {
      if (row.size() != 3) throw new WdkModelException("Enum vocab includes a row that does not contain 3 columns");
      JSONArray jsonRow = new JSONArray();
      jsonRow.put(row.get(0));
      jsonRow.put(row.get(1));
      jsonRow.put(row.get(2));
      jsonRows.put(jsonRow);
    }
    return jsonRows;
  }

  protected JSONObject getVocabTreeJson(EnumParamVocabInstance vocabInstance) {
    EnumParamTermNode[] rootNodes = vocabInstance.getVocabTreeRoots();

    // Use single root node if it has children (the root node is hidden)
    if (rootNodes.length == 1 && rootNodes[0].getChildren().length != 0) {
      return nodeToJson(rootNodes[0]);
    }

    EnumParamTermNode root = new EnumParamTermNode(DUMMY_VALUE);
    root.setDisplay(DUMMY_VALUE);
    for (EnumParamTermNode child: rootNodes) {
      root.addChild(child);
    }
    return nodeToJson(root);
  }

  protected JSONObject getVocabMapJson(EnumParamVocabInstance vocabInstance) {
    return new JSONObject(vocabInstance.getVocabMap());
  }

  protected JSONObject nodeToJson(EnumParamTermNode node) {
    return new JSONObject()
        .put(JsonKeys.DATA, new JSONObject().put(JsonKeys.TERM, node.getTerm()).put(JsonKeys.DISPLAY, node.getDisplay()))
        .put(JsonKeys.CHILDREN, new JSONArray(Arrays.stream(node.getChildren()).map(this::nodeToJson).toArray()));
  }

  @Override
  public String getParamType() {
    return JsonKeys.VOCAB_PARAM_TYPE;
  }

}
