package org.gusdb.wdk.model.query.param;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Stack;

import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.json.JSONException;

/**
 * @author xingao
 * 
 *         The input to can be a comma delimited list of terms, or a compressed
 *         version of it.
 * 
 *         The output is a comma delimited list of internal values, single
 *         quoted as needed.
 */
public abstract class AbstractEnumParam extends Param {

    protected abstract void initVocabMap() throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException;

    protected boolean multiPick = false;
    protected Map<String, String> termInternalMap;
    protected Map<String, String> termDisplayMap;
    protected List<EnumParamTermNode> termTreeList;

    protected boolean quote = true;

    private List<ParamConfiguration> useTermOnlies = new ArrayList<ParamConfiguration>();
    protected boolean useTermOnly = false;

    private String displayType;

    public AbstractEnumParam() {}

    public AbstractEnumParam(AbstractEnumParam param) {
        super(param);
        this.multiPick = param.multiPick;
        if (param.termDisplayMap != null)
            this.termDisplayMap = new LinkedHashMap<String, String>(
                    param.termDisplayMap);
        if (param.termInternalMap != null)
            this.termInternalMap = new LinkedHashMap<String, String>(
                    param.termInternalMap);
        if (param.termTreeList != null) {
            this.termTreeList = new ArrayList<EnumParamTermNode>(
                    param.termTreeList);
        }
        this.quote = param.quote;
        this.useTermOnly = param.useTermOnly;
        this.displayType = param.displayType;
    }

    // ///////////////////////////////////////////////////////////////////
    // /////////// Public properties ////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////

    public void setMultiPick(Boolean multiPick) {
        this.multiPick = multiPick.booleanValue();
    }

    public Boolean getMultiPick() {
        return new Boolean(multiPick);
    }

    public void setQuote(boolean quote) {
        this.quote = quote;
    }

    public boolean getQuote() {
        return quote;
    }

    public void addUseTermOnly(ParamConfiguration paramConfig) {
        this.useTermOnlies.add(paramConfig);
    }

    @Override
    public void validateValue(String termList) throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        // try to get term array
        getTerms(termList);
    }

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.Param#getInternalValue(java.lang.String)
     */
    @Override
    public String getInternalValue(String termList) throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        // the input is a list of terms
        String[] terms = getTerms(termList);

        StringBuffer buf = new StringBuffer();
        for (String term : terms) {
            String internal = useTermOnly ? term : termInternalMap.get(term);
            if (quote) internal = "'" + internal + "'";
            if (buf.length() != 0) buf.append(", ");
            buf.append(internal);
        }
        return buf.toString();
    }

    public String[] getVocab() throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        initVocabMap();
        String[] array = new String[termInternalMap.size()];
        termInternalMap.keySet().toArray(array);
        return array;
    }

    public EnumParamTermNode[] getVocabTreeRoots()
            throws NoSuchAlgorithmException, WdkModelException, SQLException,
            JSONException, WdkUserException {
        initVocabMap();
        EnumParamTermNode[] array = new EnumParamTermNode[termTreeList.size()];
        termTreeList.toArray(array);
        return array;
    }

    public String[] getVocabInternal() throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        initVocabMap();
        String[] array = new String[termInternalMap.size()];
        if (useTermOnly) termInternalMap.keySet().toArray(array);
        else termInternalMap.values().toArray(array);
        return array;
    }

    public String[] getDisplays() throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        Map<String, String> displayMap = getDisplayMap();
        String[] displays = new String[displayMap.size()];
        displayMap.values().toArray(displays);
        return displays;
    }

    public Map<String, String> getVocabMap() throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        initVocabMap();
        Map<String, String> newVocabMap = new LinkedHashMap<String, String>();
        for (String term : termInternalMap.keySet()) {
            newVocabMap.put(term, useTermOnly ? term
                    : termInternalMap.get(term));
        }
        return newVocabMap;
    }

    public Map<String, String> getDisplayMap() throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        initVocabMap();
        Map<String, String> newDisplayMap = new LinkedHashMap<String, String>();
        for (String term : termDisplayMap.keySet()) {
            newDisplayMap.put(term, termDisplayMap.get(term));
        }
        return newDisplayMap;
    }

    @Override
    public String getDefault() throws WdkModelException,
            NoSuchAlgorithmException, SQLException, JSONException,
            WdkUserException {
        if (defaultValue == null || defaultValue.length() == 0) {
            // select a tree branch by default
            EnumParamTermNode[] roots = getVocabTreeRoots();
            StringBuffer buffer = new StringBuffer();
            Stack<EnumParamTermNode> stack = new Stack<EnumParamTermNode>();
            stack.push(roots[0]);
            while (!stack.empty()) {
                EnumParamTermNode node = stack.pop();
                if (buffer.length() > 0) buffer.append(",");
                buffer.append(node.getTerm());
                
                for(EnumParamTermNode child : node.getChildren()) {
                    stack.push(child);
                }
            }
            return buffer.toString();
        } else return defaultValue;
    }

    /**
     * @return the useTermOnly
     */
    public boolean isUseTermOnly() {
        return this.useTermOnly;
    }

    /**
     * @param useTermOnly
     */
    public void setUseTermOnly(boolean useTermOnly) {
        this.useTermOnly = useTermOnly;
    }

    /**
     * @return the displayType
     */
    public String getDisplayType() {
        return displayType;
    }

    /**
     * @param displayType
     *            the displayType to set
     */
    public void setDisplayType(String displayType) {
        this.displayType = displayType;
    }

    // ///////////////////////////////////////////////////////////////////
    // /////////// Protected properties ////////////////////////////////////
    // ///////////////////////////////////////////////////////////////////

    /*
     * (non-Javadoc)
     * 
     * @see org.gusdb.wdk.model.Param#excludeResources(java.lang.String)
     */
    @Override
    public void excludeResources(String projectId) throws WdkModelException {
        super.excludeResources(projectId);

        // exclude userTermOnly
        boolean hasUseTermOnly = false;
        for (ParamConfiguration paramConfig : useTermOnlies) {
            if (paramConfig.include(projectId)) {
                if (hasUseTermOnly) {
                    throw new WdkModelException("More than one <useTermOnly> "
                            + "are included in param " + getFullName());
                } else {
                    this.useTermOnly = paramConfig.isValue();
                    hasUseTermOnly = true;
                }
            }
        }
        // if no useTermOnly setting, use parent's
        if (!hasUseTermOnly) useTermOnly = paramSet.isUseTermOnly();
        useTermOnlies = null;
    }

    protected void initTreeMap(Map<String, String> termParentMap)
            throws WdkModelException {
        termTreeList = new ArrayList<EnumParamTermNode>();

        // construct index
        Map<String, EnumParamTermNode> indexMap = new LinkedHashMap<String, EnumParamTermNode>();
        for (String term : termParentMap.keySet()) {
            EnumParamTermNode node = new EnumParamTermNode(term);
            node.setDisplay(termDisplayMap.get(term));
            indexMap.put(term, node);

            // check if the node is root
            String parentTerm = termParentMap.get(term);
            if (parentTerm != null && !termInternalMap.containsKey(parentTerm))
                parentTerm = null;
            if (parentTerm == null) {
                termTreeList.add(node);
                termParentMap.put(term, parentTerm);
            }
        }
        // construct the relationships
        for (String term : termParentMap.keySet()) {
            String parentTerm = termParentMap.get(term);
            // skip if parent doesn't exist
            if (parentTerm == null) continue;

            EnumParamTermNode node = indexMap.get(term);
            EnumParamTermNode parent = indexMap.get(parentTerm);
            parent.addChild(node);
        }
    }

    public String[] getTerms(String termList) throws NoSuchAlgorithmException,
            WdkModelException, SQLException, JSONException, WdkUserException {
        // check if null value is allowed
        if (allowEmpty && termList == null) termList = getEmptyValue();

        termList = decompressValue(termList);

        // the input is a list of terms
        String[] terms;
        if (multiPick) {
            terms = termList.split(",");
            for (int i = 0; i < terms.length; i++)
                terms[i] = terms[i].trim();
        } else terms = new String[] { termList.trim() };

        // the terms has to have some value
        if (terms.length == 0)
            throw new WdkModelException("The input '" + termList
                    + "' to param '" + getFullName() + "' is invalid.");

        initVocabMap();
        for (String term : terms) {
            if (!termInternalMap.containsKey(term))
                throw new WdkModelException(" - Invalid term '" + term
                        + "' for parameter '" + name + "'");
        }
        return terms;
    }

    /**
     * Enum param value is by default independent, but we may need to compress
     * it
     * 
     * @throws WdkModelException
     * @throws NoSuchAlgorithmException
     */
    @Override
    protected String getUserIndependentValue(String value)
            throws NoSuchAlgorithmException, WdkModelException {
        if (value != null && value.length() > Utilities.MAX_PARAM_VALUE_SIZE) {
            value = queryFactory.makeClobChecksum(value);
        }
        return value;
    }
}
