package org.gusdb.wdk.model;

import java.util.Map;
import java.util.LinkedHashMap;
import java.util.Vector;
import java.util.logging.Logger;
import java.util.Iterator;

/**
 * Answer.java
 *
 * Created: Fri June 4 13:01:30 2004 EDT
 *
 * @author David Barkan
 * @version $Revision$ $Date$ $Author$
 */

public class Answer {

    private static final Logger logger = WdkLogManager.getLogger("org.gusdb.wdk.model.Answer");
    
    // ------------------------------------------------------------------
    // Instance variables
    // ------------------------------------------------------------------

    int currentRecordInstanceCounter;

    private QueryInstance queryInstance;

    private RecordInstance[] recordInstances;

    private String listPrimaryKeyName;

    private Question question;

    private int startRow;

    private int endRow;

    private boolean isBoolean = false;

    private Integer resultSize;   // size of total result

    // ------------------------------------------------------------------
    // Constructor
    // ------------------------------------------------------------------

    /**
     * Assumes the values of <code>queryInstance</code> have been set already.
     */
    Answer(Question question, QueryInstance queryInstance, int startRow, int endRow) throws WdkUserException, WdkModelException{

	this.question = question;
	this.queryInstance = queryInstance;
	if (queryInstance instanceof org.gusdb.wdk.model.BooleanQueryInstance){
	    this.isBoolean = true;
	}
	this.currentRecordInstanceCounter = 0;
	this.startRow = startRow;
	this.endRow = endRow;   
	ResultList rl = getRecordInstanceIds();
	rl.close(); // rl only needed to close connection
    }

    // ------------------------------------------------------------------
    // Public Methods
    // ------------------------------------------------------------------

    /**
     * provide property that user's term for question
     */
    public Question getQuestion(){
	return this.question;
    }

    public int getPageSize(){
        if (recordInstances != null) {
            return recordInstances.length;
        }
        return 0;
    }
    
    public int getResultSize() throws WdkModelException{

	if (resultSize == null) {
	    ResultList rl = getRecordInstanceIds();
	    int counter = 0;
	    while (rl.next()){
		counter++;
	    }
	    rl.close();
	    resultSize = new Integer(counter);
	}
	return resultSize.intValue();
    }

    /**
     * @return Map where key is param name and value is param value
     */
    public Map getParams() {
	return queryInstance.getValuesMap();
    }

    /**
     * @return Map where key is param display name and value is param value
     */
    public Map getDisplayParams() {
	LinkedHashMap displayParamsMap = new LinkedHashMap();
	Map paramsMap = getParams();
	Param[] params = question.getParams();
	for (int i=0; i<params.length; i++) {
	    Param param = params[i];
	    displayParamsMap.put(param.getPrompt(), 
				 paramsMap.get(param.getName()));
	}
	return displayParamsMap;
    }


    //Returns null if we have already returned the last instance
    public RecordInstance getNextRecordInstance() throws WdkModelException{
	
	if (recordInstances == null){
	    initRecordInstances();
	}
	RecordInstance nextInstance = null;
	if (currentRecordInstanceCounter < recordInstances.length){
	    nextInstance = recordInstances[currentRecordInstanceCounter];
	    currentRecordInstanceCounter++;
	}
	return nextInstance;
    }
    
    public boolean hasMoreRecordInstances() throws WdkModelException 
    {
        if (recordInstances == null){
	    initRecordInstances();
        }
        if (recordInstances == null){
            logger.finer("recordInstances is still null");
        }
        if (currentRecordInstanceCounter < recordInstances.length){
            return true;
        }
        return false;
    }

    public void print() throws WdkModelException{  
	
	if (recordInstances == null){
	    initRecordInstances();
	}
	for (int i = 0; i < recordInstances.length; i++){
	    System.out.println(recordInstances[i].print());
	}
    }

    public void printAsTable() throws WdkModelException{
	
	if (recordInstances == null){
	    initRecordInstances();
	}
	if (recordInstances != null && recordInstances.length > 0){
	    RecordClass recordClass = recordInstances[0].getRecordClass();
	    Iterator attributeNames = 
		recordClass.getAttributeFields().keySet().iterator();
	    StringBuffer heading = new  StringBuffer();
	    while (attributeNames.hasNext()){
		heading.append((String)attributeNames.next() + "\t");
	    }
	    System.out.println(heading);

	    for (int i = 0; i < recordInstances.length; i++){
		StringBuffer recordLine = new StringBuffer();
		RecordInstance recordInstance = recordInstances[i];
		attributeNames = 
		    recordInstance.getAttributes().keySet().iterator();
		while (attributeNames.hasNext()){
		    Object value = 
			recordInstance.getAttributeValue((String)attributeNames.next());
		    recordLine.append(value.toString() + "\t");
		}
		System.out.println(recordLine);
	    }
	}
    }

    private void initRecordInstances() throws WdkModelException {
	ResultList rl = getRecordInstanceIds();
	Query query = queryInstance.getQuery();
	Vector tempRecordInstances = new Vector();
	int counter = 0;
	while (rl.next()){
	    counter++;
	    if (counter >= startRow && counter <= endRow){
		RecordInstance nextRecordInstance = getQuestion().getRecordClass().makeRecordInstance();
		
		Column[] columns = query.getColumns();
		String primaryKeyName = columns[0].getName();
		this.listPrimaryKeyName = primaryKeyName;
		String primaryKey = 
		    rl.getAttributeFieldValue(primaryKeyName).getValue().toString();
		nextRecordInstance.setPrimaryKey(primaryKey);
		
		nextRecordInstance.setAnswer(this);
		tempRecordInstances.add(nextRecordInstance);
	    }
	}        
	recordInstances = new RecordInstance[tempRecordInstances.size()];
	tempRecordInstances.copyInto(recordInstances);
	rl.close();
    }

    public boolean getIsBoolean(){
	return this.isBoolean;
    }

    // ------------------------------------------------------------------
    // Package Methods
    // ------------------------------------------------------------------
    
    void setMultiMode(QueryInstance instance) throws WdkModelException{
        
        String resultTableName = queryInstance.getResultAsTable();
        
        instance.setMultiModeValues(resultTableName, listPrimaryKeyName, startRow, endRow);
    }
    
    void setQueryResult(ResultList resultList) throws WdkModelException {

        int tempCounter = 0;
        while (resultList.next()){
            
            RecordInstance nextRecordInstance = recordInstances[tempCounter];
            Query query = resultList.getQuery();
            Column[] columns = query.getColumns();
            for (int j = 0; j < columns.length; j++){
                String nextColumnName = columns[j].getName();

                Object value = 
		    resultList.getAttributeFieldValue(nextColumnName).getValue();
                nextRecordInstance.setAttributeValue(nextColumnName, value);
            }
            tempCounter++;
        }
    }
    
    QueryInstance getQueryInstance(){
	return queryInstance;
    }

    // ------------------------------------------------------------------
    // Private Methods
    // ------------------------------------------------------------------
    
    private ResultList getRecordInstanceIds() throws WdkModelException{

	ResultList rl = queryInstance.getResult();
	return rl;
    }
    



}
