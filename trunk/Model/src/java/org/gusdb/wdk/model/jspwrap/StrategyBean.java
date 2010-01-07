package org.gusdb.wdk.model.jspwrap;

import java.sql.SQLException;
import java.security.NoSuchAlgorithmException;
import java.text.DateFormat;
import java.util.Date;
import java.util.Map;

import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.user.Step;
import org.gusdb.wdk.model.user.Strategy;
import org.json.JSONException;

public class StrategyBean {

    private UserBean user;
    Strategy strategy;

    public StrategyBean(UserBean user, Strategy strategy) {
        this.user = user;
        this.strategy = strategy;
    }

    public UserBean getUser() {
        return user;
    }

    public boolean getIsDeleted() {
        return strategy.isDeleted();
    }

    public String getVersion() {
        return strategy.getVersion();
    }

    public String getName() {
        return strategy.getName();
    }

    public void setName(String name) {
        strategy.setName(name);
    }

    public String getSavedName() {
        return strategy.getSavedName();
    }

    public void setSavedName(String name) {
        strategy.setSavedName(name);
    }

    public void setIsSaved(boolean saved) {
        strategy.setIsSaved(saved);
    }

    public boolean getIsSaved() {
        return strategy.getIsSaved();
    }

    public String getLastRunTimeFormatted() {
        return formatDate(strategy.getLastRunTime());
    }

    public Date getLastRunTime() {
        return strategy.getLastRunTime();
    }

    public String getCreatedTimeFormatted() {
        return formatDate(strategy.getCreatedTime());
    }

    public Date getCreatedTime() {
        return strategy.getCreatedTime();
    }

    public String getLastModifiedTimeFormatted() {
        return formatDate(strategy.getLastModifiedTime());
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.user.Strategy#getLastModifiedTime()
     */
    public Date getLastModifiedTime() {
        return strategy.getLastModifiedTime();
    }

    public String getLastViewedTimeFormatted() {
        return formatDate(strategy.getLastViewedTime());
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.user.Strategy#getLastViewedTime()
     */
    public Date getLastViewedTime() {
        return strategy.getLastViewedTime();
    }

    public StepBean getLatestStep() {
        return new StepBean(user, strategy.getLatestStep());
    }

    public int getStrategyId() {
        return strategy.getStrategyId();
    }

    public int getInternalId() {
        return strategy.getInternalId();
    }

    public StepBean getStep(int index) {
        StepBean latestStep = new StepBean(user, strategy.getLatestStep());
        return latestStep.getStep(index);
    }

    public StepBean[] getAllSteps() {
        StepBean latestStep = new StepBean(user, strategy.getLatestStep());
        return latestStep.getAllSteps();
    }

    public void addStep(StepBean step) throws WdkUserException,
            WdkModelException, SQLException, JSONException {
        strategy.addStep(step.step);
    }

    public void setLatestStep(StepBean step) throws WdkUserException,
            WdkModelException, SQLException, JSONException {
        strategy.setLatestStep(step.step);
    }

    public StepBean getStepById(int stepId) {
        Step target = strategy.getStepById(stepId);
        if (target != null) {
            return new StepBean(user, target);
        }
        return null;
    }

    public int getLength() {
        return getAllSteps().length;
    }

    public void update(boolean overwrite) throws WdkUserException,
            WdkModelException, SQLException, JSONException,
            NoSuchAlgorithmException {
        strategy.update(overwrite);
    }

    public Map<Integer, Integer> addStep(int targetStepId, StepBean step)
            throws WdkModelException, WdkUserException, JSONException,
            NoSuchAlgorithmException, SQLException {
        return strategy.addStep(targetStepId, step.step);
    }

    public Map<Integer, Integer> editOrInsertStep(int targetStepId,
            StepBean step) throws WdkModelException, WdkUserException,
            JSONException, NoSuchAlgorithmException, SQLException {
        return strategy.editOrInsertStep(targetStepId, step.step);
    }

    public Map<Integer, Integer> moveStep(int moveFromId, int moveToId,
            String branch) throws WdkModelException, WdkUserException,
            JSONException, NoSuchAlgorithmException, SQLException {
        return strategy.moveStep(moveFromId, moveToId, branch);
    }

    public Map<Integer, Integer> deleteStep(int stepId, boolean isBranch)
            throws WdkModelException, WdkUserException, JSONException,
            NoSuchAlgorithmException, SQLException {
        return strategy.deleteStep(stepId, isBranch);
    }

    public String getImportId() throws NoSuchAlgorithmException, JSONException,
            WdkModelException {
        return strategy.getSignature();
    }

    public StepBean getFirstStep() {
        return new StepBean(user, strategy.getFirstStep());
    }

    public String getChecksum() throws NoSuchAlgorithmException, JSONException,
            WdkModelException {
        return strategy.getChecksum();
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.user.Strategy#getDescription()
     */
    public String getDescription() {
        return strategy.getDescription();
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.user.Strategy#getSignature()
     */
    public String getSignature() {
        return strategy.getSignature();
    }

    private String formatDate(Date date) {
        int style = DateFormat.SHORT;
        return DateFormat.getDateTimeInstance(style, style).format(date);
    }

    /**
     * @return
     * @see org.gusdb.wdk.model.user.Strategy#isValid()
     */
    public boolean isValid() {
        return strategy.isValid();
    }
    
    public String getDisplayType() {
        return strategy.getLatestStep().getDisplayType();
    }
}
