/**
 * 
 */
package org.gusdb.wdk.model.user;

import java.security.NoSuchAlgorithmException;
import java.sql.SQLException;

import org.gusdb.wdk.model.AnswerValue;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.json.JSONException;

/**
 * @author xingao
 * 
 */
public class Answer {

    private int answerId;
    private String answerChecksum;
    private String projectId;
    private String projectVersion;
    private String questionName;
    private String queryChecksum;

    private AnswerFactory answerFactory;
    private AnswerValue answerValue;

    Answer(AnswerFactory answerFactory, int answerId) {
        this.answerFactory = answerFactory;
        this.answerId = answerId;
    }

    /**
     * @return the answerChecksum
     */
    public String getAnswerChecksum() {
        return answerChecksum;
    }

    /**
     * @param answerChecksum
     *            the answerChecksum to set
     */
    public void setAnswerChecksum(String answerChecksum) {
        this.answerChecksum = answerChecksum;
    }

    /**
     * @return the projectId
     */
    public String getProjectId() {
        return projectId;
    }

    /**
     * @param projectId
     *            the projectId to set
     */
    public void setProjectId(String projectId) {
        this.projectId = projectId;
    }

    /**
     * @return the projectVersion
     */
    public String getProjectVersion() {
        return projectVersion;
    }

    /**
     * @param projectVersion
     *            the projectVersion to set
     */
    public void setProjectVersion(String projectVersion) {
        this.projectVersion = projectVersion;
    }

    /**
     * @return the questionName
     */
    public String getQuestionName() {
        return questionName;
    }

    /**
     * @param questionName
     *            the questionName to set
     */
    public void setQuestionName(String questionName) {
        this.questionName = questionName;
    }

    /**
     * @return the queryChecksum
     */
    public String getQueryChecksum() {
        return queryChecksum;
    }

    /**
     * @param queryChecksum
     *            the queryChecksum to set
     */
    public void setQueryChecksum(String queryChecksum) {
        this.queryChecksum = queryChecksum;
    }

    /**
     * @return the answerId
     */
    public int getAnswerId() {
        return answerId;
    }

    public AnswerValue getAnswerValue() throws NoSuchAlgorithmException,
            WdkModelException, JSONException, WdkUserException, SQLException {
        if (answerValue == null) {
            answerValue = answerFactory.getAnswerValue(this);
            answerValue.setAnswer(this);
        }
        return answerValue;
    }

    public void setAnswerValue(AnswerValue answerValue) {
        this.answerValue = answerValue;
    }
}
