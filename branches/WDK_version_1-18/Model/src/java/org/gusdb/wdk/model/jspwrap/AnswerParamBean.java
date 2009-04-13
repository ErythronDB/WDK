/**
 * 
 */
package org.gusdb.wdk.model.jspwrap;

import java.sql.SQLException;

import org.gusdb.wdk.model.Answer;
import org.gusdb.wdk.model.AnswerParam;
import org.gusdb.wdk.model.RecordClass;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.json.JSONException;

/**
 * @author xingao
 * 
 */
public class AnswerParamBean extends ParamBean {

    private AnswerParam answerParam;

    private String answerKey;
    private String historyKey;

    public AnswerParamBean(AnswerParam answerParam) {
        super(answerParam);
        this.answerParam = answerParam;
    }

    public HistoryBean[] getHistories(UserBean user) throws WdkUserException,
            WdkModelException, SQLException, JSONException {
        RecordClass recordClass = answerParam.getRecordClass();
        return user.getHistories(recordClass.getFullName());
    }

    /**
     * It's okay to set bean property, since the bean is created for every
     * request and thus has a local (request) level life span.
     * 
     * @param checksum
     */
    public void setAnswerChecksum(String checksum) {
        this.answerKey = checksum;
    }

    public void setHistoryKey(String historyKey) {
        this.historyKey = historyKey;
    }

    public AnswerBean getAnswer() throws Exception {
        try {
            if (historyKey != null)
                answerKey = answerParam.getInternalValue(historyKey);

            Answer answer = answerParam.getAnswer(answerKey);
            return new AnswerBean(answer);
        } catch (Exception ex) {
            ex.printStackTrace();
            throw ex;
        }
    }
}
