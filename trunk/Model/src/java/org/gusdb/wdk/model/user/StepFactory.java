/**
 * 
 */
package org.gusdb.wdk.model.user;

import java.security.NoSuchAlgorithmException;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;
import java.util.Stack;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.AnswerFilterInstance;
import org.gusdb.wdk.model.AnswerValue;
import org.gusdb.wdk.model.ModelConfigUserDB;
import org.gusdb.wdk.model.Question;
import org.gusdb.wdk.model.RecordClass;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.dbms.SqlUtils;
import org.gusdb.wdk.model.query.BooleanQuery;
import org.gusdb.wdk.model.query.Query;
import org.gusdb.wdk.model.query.param.AnswerParam;
import org.gusdb.wdk.model.query.param.DatasetParam;
import org.gusdb.wdk.model.query.param.Param;
import org.gusdb.wdk.model.query.param.StringParam;
import org.json.JSONException;
import org.json.JSONObject;

/**
 * @author xingao
 * 
 */
public class StepFactory {

    private static final String TABLE_STEP = "steps";
    private static final String TABLE_STRATEGY = "strategies";

    private static final String COLUMN_STEP_INTERNAL_ID = "step_id";
    private static final String COLUMN_DISPLAY_ID = "display_id";
    private static final String COLUMN_LEFT_CHILD_ID = "left_child_id";
    private static final String COLUMN_RIGHT_CHILD_ID = "right_child_id";
    private static final String COLUMN_CREATE_TIME = "create_time";
    private static final String COLUMN_LAST_RUN_TIME = "last_run_time";
    private static final String COLUMN_ESTIMATE_SIZE = "estimate_size";
    private static final String COLUMN_ANSWER_FILTER = "answer_filter";
    private static final String COLUMN_CUSTOM_NAME = "custom_name";
    private static final String COLUMN_IS_DELETED = "is_deleted";
    private static final String COLUMN_COLLAPSED_NAME = "collapsed_name";
    private static final String COLUMN_IS_COLLAPSIBLE = "is_collapsible";
    private static final String COLUMN_DISPLAY_PARAMS = "display_params";

    private static final String COLUMN_STRATEGY_INTERNAL_ID = "strategy_id";
    private static final String COLUMN_ROOT_STEP_ID = "root_step_id";
    private static final String COLUMN_PROJECT_ID = "project_id";
    private static final String COLUMN_IS_SAVED = "is_saved";
    private static final String COLUMN_NAME = "name";
    private static final String COLUMN_SAVED_NAME = "saved_name";

    private static final Logger logger = Logger.getLogger(StepFactory.class);

    private WdkModel wdkModel;
    private String userSchema;
    private String wdkSchema;
    private DBPlatform userPlatform;
    private DataSource dataSource;

    public StepFactory(WdkModel wdkModel) {
        this.wdkModel = wdkModel;
        this.userPlatform = wdkModel.getUserPlatform();
        dataSource = userPlatform.getDataSource();

        ModelConfigUserDB userDB = wdkModel.getModelConfig().getUserDB();
        this.userSchema = userDB.getUserSchema();
        this.wdkSchema = userDB.getWdkEngineSchema();
    }

    // parse boolexp to pass left_child_id, right_child_id to loadAnswer
    Step createStep(User user, Question question,
            Map<String, String> dependentValues, AnswerFilterInstance filter,
            int pageStart, int pageEnd, boolean deleted) throws SQLException,
            WdkModelException, NoSuchAlgorithmException, WdkUserException,
            JSONException {

        // get summary list and sorting list
        String questionName = question.getFullName();
        Map<String, Boolean> sortingAttributes = user.getSortingAttributes(questionName);
        String[] summaryAttributes = user.getSummaryAttributes(questionName);

        // create answer
        AnswerValue answerValue = question.makeAnswerValue(user,
                dependentValues, pageStart, pageEnd, sortingAttributes, filter);
        if (summaryAttributes != null) {
            answerValue.setSumaryAttributes(summaryAttributes);
        }
        Answer answer = answerValue.getAnswer();

        // prepare the values to be inserted.
        int userId = user.getUserId();
        int answerId = answer.getAnswerId();

        String filterName = null;
        int estimateSize;
        if (filter != null) {
            filterName = filter.getName();
            estimateSize = answerValue.getFilterSize(filterName);
        } else estimateSize = answerValue.getResultSize();

        String displayParamContent = getParamContent(dependentValues);

        // prepare SQLs
        String userIdColumn = UserFactory.COLUMN_USER_ID;
        String answerIdColumn = AnswerFactory.COLUMN_ANSWER_ID;

        StringBuffer sqlMaxId = new StringBuffer("SELECT max(");
        sqlMaxId.append(COLUMN_DISPLAY_ID).append(") AS max_id FROM ");
        sqlMaxId.append(userSchema).append(TABLE_STEP).append(" s, ");
        sqlMaxId.append(wdkSchema).append(AnswerFactory.TABLE_ANSWER).append(
                " a ");
        sqlMaxId.append("WHERE s.").append(userIdColumn).append(" = ").append(
                userId);
        sqlMaxId.append(" AND s.").append(answerIdColumn);
        sqlMaxId.append(" = a.").append(answerIdColumn);

        StringBuffer sqlInsertStep = new StringBuffer("INSERT INTO ");
        sqlInsertStep.append(userSchema).append(TABLE_STEP).append(" (");
        sqlInsertStep.append(COLUMN_STEP_INTERNAL_ID).append(", ");
        sqlInsertStep.append(COLUMN_DISPLAY_ID).append(", ");
        sqlInsertStep.append(userIdColumn).append(", ");
        sqlInsertStep.append(answerIdColumn).append(", ");
        sqlInsertStep.append(COLUMN_CREATE_TIME).append(", ");
        sqlInsertStep.append(COLUMN_LAST_RUN_TIME).append(", ");
        sqlInsertStep.append(COLUMN_ESTIMATE_SIZE).append(", ");
        sqlInsertStep.append(COLUMN_ANSWER_FILTER).append(", ");
        sqlInsertStep.append(COLUMN_IS_DELETED).append(", ");
        sqlInsertStep.append(COLUMN_DISPLAY_PARAMS).append(") ");
        sqlInsertStep.append("VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)");

        // Now that we have the Answer, create the Step
        Date createTime = new Date();
        Date lastRunTime = new Date(createTime.getTime());

        int displayId = 0;
        int stepId = userPlatform.getNextId(userSchema, TABLE_STEP);
        Connection connection = dataSource.getConnection();

        PreparedStatement psInsertStep = null;
        ResultSet rsMax = null;
        try {
            synchronized (connection) {
                connection.setAutoCommit(false);

                // get the current display id
                Statement statement = connection.createStatement();
                rsMax = statement.executeQuery(sqlMaxId.toString());
                if (rsMax.next()) // has old steps, get the max of it
                    displayId = rsMax.getInt("max_id");
                rsMax.close();
                displayId++;

                psInsertStep = connection.prepareStatement(sqlInsertStep.toString());
                psInsertStep.setInt(1, stepId);
                psInsertStep.setInt(2, displayId);
                psInsertStep.setInt(3, userId);
                psInsertStep.setInt(4, answerId);
                psInsertStep.setTimestamp(5,
                        new Timestamp(createTime.getTime()));
                psInsertStep.setTimestamp(6, new Timestamp(
                        lastRunTime.getTime()));
                psInsertStep.setInt(7, estimateSize);
                psInsertStep.setString(8, filterName);
                psInsertStep.setBoolean(9, deleted);
                userPlatform.setClobData(psInsertStep, 10, displayParamContent,
                        false);
                psInsertStep.executeUpdate();

                connection.commit();
            }
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
            SqlUtils.closeResultSet(rsMax);
            SqlUtils.closeStatement(psInsertStep);
        }
        // create the Step
        Step step = new Step(this, user, displayId, stepId);
        step.setAnswer(answer);
        step.setCreatedTime(createTime);
        step.setLastRunTime(lastRunTime);
        step.setDeleted(deleted);
        step.setDisplayParams(dependentValues);

        // update step dependencies
        updateStepTree(user, step);
        step.setValid(true);

        return step;
    }

    void deleteStep(User user, int displayId) throws WdkUserException,
            SQLException, WdkModelException {
        PreparedStatement psHistory = null;
        try {
            if (!isStepDepended(user, displayId)) {
                // remove step
                psHistory = SqlUtils.getPreparedStatement(dataSource, "DELETE "
                        + "FROM " + userSchema + TABLE_STEP + " WHERE "
                        + UserFactory.COLUMN_USER_ID + " = ? AND "
                        + COLUMN_DISPLAY_ID + " = ?");
            } else { // hide the step
                psHistory = SqlUtils.getPreparedStatement(dataSource, "UPDATE "
                        + userSchema + TABLE_STEP + " SET " + COLUMN_IS_DELETED
                        + " = 1 WHERE " + UserFactory.COLUMN_USER_ID + " = ? "
                        + " AND " + COLUMN_DISPLAY_ID + " = ?");
            }
            psHistory.setInt(1, user.getUserId());
            psHistory.setInt(2, displayId);
            int result = psHistory.executeUpdate();
            if (result == 0)
                throw new WdkUserException("The Step #" + displayId
                        + " of user " + user.getEmail() + " cannot be found.");
        } finally {
            SqlUtils.closeStatement(psHistory);
        }
    }

    boolean isStepDepended(User user, int displayId) throws SQLException,
            WdkModelException {
        String answerIdColumn = AnswerFactory.COLUMN_ANSWER_ID;
        StringBuffer sql = new StringBuffer("SELECT count(*) FROM ");
        sql.append(userSchema).append(TABLE_STEP).append(" s, ");
        sql.append(wdkSchema).append(AnswerFactory.TABLE_ANSWER).append(" a ");
        sql.append(" WHERE s.").append(UserFactory.COLUMN_USER_ID);
        sql.append(" = ").append(user.getUserId());
        sql.append(" AND s.").append(answerIdColumn);
        sql.append(" = a.").append(answerIdColumn);
        sql.append(" AND (").append(COLUMN_LEFT_CHILD_ID);
        sql.append(" = ").append(displayId);
        sql.append(" OR ").append(COLUMN_RIGHT_CHILD_ID);
        sql.append(" = ").append(displayId).append(")");

        Object result = SqlUtils.executeScalar(dataSource, sql.toString());
        int count = Integer.parseInt(result.toString());
        return (count > 0);
    }

    void deleteSteps(User user, boolean allProjects) throws WdkUserException,
            SQLException {
        PreparedStatement psDeleteSteps = null;
        String stepTable = userSchema + TABLE_STEP;
        String answerTable = wdkSchema + AnswerFactory.TABLE_ANSWER;
        String strategyTable = userSchema + TABLE_STRATEGY;
        String userIdColumn = UserFactory.COLUMN_USER_ID;
        String answerIdColumn = AnswerFactory.COLUMN_ANSWER_ID;
        String projectIdColumn = AnswerFactory.COLUMN_PROJECT_ID;
        try {
            StringBuffer sql = new StringBuffer("DELETE FROM ");
            sql.append(stepTable).append(" WHERE ");
            sql.append(userIdColumn).append(" = ? ");
            if (!allProjects) {
                sql.append(" AND ").append(answerIdColumn).append(" IN (");
                sql.append(" SELECT s.").append(answerIdColumn);
                sql.append(" FROM ").append(stepTable).append(" s, ");
                sql.append(answerTable).append(" a ");
                sql.append(" WHERE s.").append(answerIdColumn);
                sql.append(" = a.").append(answerIdColumn);
                sql.append(" AND a.").append(projectIdColumn).append(" = ?) ");
            }
            sql.append(" AND ").append(COLUMN_DISPLAY_ID);
            sql.append(" NOT IN (SELECT ").append(COLUMN_ROOT_STEP_ID);
            sql.append(" FROM ").append(strategyTable);
            if (!allProjects) {
                sql.append(" WHERE ").append(COLUMN_PROJECT_ID).append(" = ? ");
                sql.append(" AND ").append(userIdColumn).append(" = ? ");
            }
            sql.append(") ");
            psDeleteSteps = SqlUtils.getPreparedStatement(dataSource,
                    sql.toString());

            psDeleteSteps.setInt(1, user.getUserId());
            if (!allProjects) {
                String projectId = wdkModel.getProjectId();
                psDeleteSteps.setString(2, projectId);
                psDeleteSteps.setString(3, projectId);
                psDeleteSteps.setInt(4, user.getUserId());
            }
            psDeleteSteps.executeUpdate();
        } finally {
            SqlUtils.closeStatement(psDeleteSteps);
        }
    }

    public void deleteStrategy(User user, int displayId)
            throws WdkUserException, SQLException {
        PreparedStatement psStrategy = null;
        try {
            // remove history
            /*
             * psStrategy = SqlUtils.getPreparedStatement(dataSource, "DELETE "
             * + "FROM " + userSchema + TABLE_STRATEGY + " WHERE " +
             * UserFactory.COLUMN_USER_ID + " = ? " + "AND " + COLUMN_PROJECT_ID
             * + " = ? AND " + COLUMN_DISPLAY_ID + " = ?"); psStrategy.setInt(1,
             * user.getUserId()); psStrategy.setString(2,
             * wdkModel.getProjectId()); psStrategy.setInt(3, displayId); int
             * result = psStrategy.executeUpdate(); if (result == 0) throw new
             * WdkUserException("The strategy #" + displayId + " of user " +
             * user.getEmail() + " cannot be found.");
             */
            psStrategy = SqlUtils.getPreparedStatement(dataSource, "UPDATE "
                    + userSchema + TABLE_STRATEGY + " SET " + COLUMN_IS_DELETED
                    + " = ? WHERE " + UserFactory.COLUMN_USER_ID + " = ? "
                    + "AND " + COLUMN_PROJECT_ID + " = ? AND "
                    + COLUMN_DISPLAY_ID + " = ?");
            psStrategy.setBoolean(1, true);
            psStrategy.setInt(2, user.getUserId());
            psStrategy.setString(3, wdkModel.getProjectId());
            psStrategy.setInt(4, displayId);
            int result = psStrategy.executeUpdate();
            if (result == 0)
                throw new WdkUserException("The strategy #" + displayId
                        + " of user " + user.getEmail() + " cannot be found.");
        } finally {
            SqlUtils.closeStatement(psStrategy);
        }
    }

    void deleteStrategies(User user, boolean allProjects) throws SQLException {
        PreparedStatement psDeleteStrategies = null;
        try {
            StringBuffer sql = new StringBuffer("DELETE FROM ");
            sql.append(userSchema).append(TABLE_STRATEGY).append(" WHERE ");
            sql.append(UserFactory.COLUMN_USER_ID).append(" = ?");
            if (!allProjects) {
                sql.append(" AND ").append(COLUMN_PROJECT_ID).append(" = ?");
            }
            psDeleteStrategies = SqlUtils.getPreparedStatement(dataSource,
                    sql.toString());

            psDeleteStrategies.setInt(1, user.getUserId());
            if (!allProjects)
                psDeleteStrategies.setString(2, wdkModel.getProjectId());
            psDeleteStrategies.executeUpdate();
        } finally {
            SqlUtils.closeStatement(psDeleteStrategies);
        }
    }

    void deleteInvalidSteps(User user) throws WdkUserException,
            WdkModelException, SQLException, JSONException {
        // get invalid histories
        Map<Integer, Step> invalidSteps = new LinkedHashMap<Integer, Step>();
        loadSteps(user, invalidSteps);
        for (int displayId : invalidSteps.keySet()) {
            deleteStep(user, displayId);
        }
    }

    void deleteInvalidStrategies(User user) throws WdkUserException,
            WdkModelException, SQLException, JSONException {
        // get invalid histories
        Map<Integer, Strategy> invalidStrategies = new LinkedHashMap<Integer, Strategy>();
        loadStrategies(user, invalidStrategies);
        for (int displayId : invalidStrategies.keySet()) {
            deleteStep(user, displayId);
        }
    }

    int getStepCount(User user) throws WdkUserException {
        String stepTable = userSchema + TABLE_STEP;
        String answerTable = wdkSchema + AnswerFactory.TABLE_ANSWER;
        String answerIdColumn = AnswerFactory.COLUMN_ANSWER_ID;
        ResultSet rsHistory = null;
        try {
            PreparedStatement psHistory = SqlUtils.getPreparedStatement(
                    dataSource, "SELECT count(h." + COLUMN_STEP_INTERNAL_ID
                            + ") AS num" + " FROM " + stepTable + " h, "
                            + answerTable + " a WHERE h." + answerIdColumn
                            + " = a." + answerIdColumn + " AND h."
                            + UserFactory.COLUMN_USER_ID + " = ? AND a."
                            + AnswerFactory.COLUMN_PROJECT_ID + " = ? "
                            + " AND is_deleted = "
                            + userPlatform.convertBoolean(false));
            psHistory.setInt(1, user.getUserId());
            psHistory.setString(2, wdkModel.getProjectId());
            rsHistory = psHistory.executeQuery();
            rsHistory.next();
            return rsHistory.getInt("num");
        } catch (SQLException ex) {
            throw new WdkUserException(ex);
        } finally {
            try {
                SqlUtils.closeResultSet(rsHistory);
            } catch (SQLException ex) {
                throw new WdkUserException(ex);
            }
        }
    }

    Map<Integer, Step> loadSteps(User user, Map<Integer, Step> invalidSteps)
            throws SQLException, WdkModelException, JSONException {
        Map<Integer, Step> steps = new LinkedHashMap<Integer, Step>();

        String answerIdColumn = AnswerFactory.COLUMN_ANSWER_ID;
        ResultSet rsStep = null;
        try {
            PreparedStatement psStep = SqlUtils.getPreparedStatement(
                    dataSource, "SELECT h." + COLUMN_STEP_INTERNAL_ID + ", h."
                            + COLUMN_DISPLAY_ID + ", a."
                            + AnswerFactory.COLUMN_ANSWER_CHECKSUM + ", h."
                            + COLUMN_CREATE_TIME + ", h."
                            + COLUMN_LAST_RUN_TIME + ", h."
                            + COLUMN_CUSTOM_NAME + ", h." + COLUMN_IS_DELETED
                            + ", h." + COLUMN_IS_COLLAPSIBLE + ", h."
                            + COLUMN_COLLAPSED_NAME + ", h."
                            + COLUMN_LEFT_CHILD_ID + ", h."
                            + COLUMN_RIGHT_CHILD_ID + ", h."
                            + COLUMN_ESTIMATE_SIZE + ", h."
                            + COLUMN_ANSWER_FILTER + ", h."
                            + COLUMN_DISPLAY_PARAMS + " FROM " + userSchema
                            + TABLE_STEP + " h, " + wdkSchema
                            + AnswerFactory.TABLE_ANSWER + " a WHERE h."
                            + UserFactory.COLUMN_USER_ID + " = ? AND h."
                            + answerIdColumn + " = a." + answerIdColumn
                            + " AND a." + AnswerFactory.COLUMN_PROJECT_ID
                            + " = ? ORDER BY h." + COLUMN_LAST_RUN_TIME
                            + " DESC");
            psStep.setInt(1, user.getUserId());
            psStep.setString(2, wdkModel.getProjectId());
            rsStep = psStep.executeQuery();

            while (rsStep.next()) {
                Step step = loadStep(user, rsStep);
                int stepId = step.getDisplayId();
                if (step.isValid()) {
                    steps.put(stepId, step);
                } else {
                    invalidSteps.put(stepId, step);
                }
            }
        } finally {
            SqlUtils.closeResultSet(rsStep);
        }
        System.out.println("Steps: " + steps.size());
        System.out.println("Invalid: " + invalidSteps.size());
        return steps;
    }

    // get left child id, right child id in here
    Step loadStep(User user, int displayId) throws WdkUserException,
            SQLException, WdkModelException, JSONException {
        String answerIdColumn = AnswerFactory.COLUMN_ANSWER_ID;
        ResultSet rsStep = null;
        try {
            PreparedStatement psStep = SqlUtils.getPreparedStatement(
                    dataSource, "SELECT h." + COLUMN_STEP_INTERNAL_ID + ", h."
                            + COLUMN_DISPLAY_ID + ", a."
                            + AnswerFactory.COLUMN_ANSWER_CHECKSUM + ", h."
                            + COLUMN_CREATE_TIME + ", h."
                            + COLUMN_LAST_RUN_TIME + ", h."
                            + COLUMN_CUSTOM_NAME + ", h." + COLUMN_IS_DELETED
                            + ", h." + COLUMN_IS_COLLAPSIBLE + ", h."
                            + COLUMN_COLLAPSED_NAME + ", h."
                            + COLUMN_LEFT_CHILD_ID + ", h."
                            + COLUMN_RIGHT_CHILD_ID + ", h."
                            + COLUMN_ESTIMATE_SIZE + ", h."
                            + COLUMN_ANSWER_FILTER + ", h."
                            + COLUMN_DISPLAY_PARAMS + " FROM " + userSchema
                            + TABLE_STEP + " h, " + wdkSchema
                            + AnswerFactory.TABLE_ANSWER + " a WHERE h."
                            + UserFactory.COLUMN_USER_ID + " = ? AND h."
                            + answerIdColumn + " = a." + answerIdColumn
                            + " AND a." + AnswerFactory.COLUMN_PROJECT_ID
                            + " = ? AND h." + COLUMN_DISPLAY_ID + " = ?");
            psStep.setInt(1, user.getUserId());
            psStep.setString(2, wdkModel.getProjectId());
            psStep.setInt(3, displayId);
            rsStep = psStep.executeQuery();
            if (!rsStep.next())
                throw new WdkUserException("The Step #" + displayId
                        + " of user " + user.getEmail() + " doesn't exist.");

            return loadStep(user, rsStep);
        } finally {
            SqlUtils.closeResultSet(rsStep);
        }
    }

    private Step loadStep(User user, ResultSet rsStep)
            throws WdkModelException, SQLException, JSONException {
        // load Step info
        int stepId = rsStep.getInt(COLUMN_STEP_INTERNAL_ID);
        int displayId = rsStep.getInt(COLUMN_DISPLAY_ID);

        Step step = new Step(this, user, displayId, stepId);
        step.setCreatedTime(rsStep.getTimestamp(COLUMN_CREATE_TIME));
        step.setLastRunTime(rsStep.getTimestamp(COLUMN_LAST_RUN_TIME));
        step.setCustomName(rsStep.getString(COLUMN_CUSTOM_NAME));
        step.setDeleted(rsStep.getBoolean(COLUMN_IS_DELETED));
        step.setCollapsible(rsStep.getBoolean(COLUMN_IS_COLLAPSIBLE));
        step.setCollapsedName(rsStep.getString(COLUMN_COLLAPSED_NAME));
        step.setEstimateSize(rsStep.getInt(COLUMN_ESTIMATE_SIZE));

        String dependentParamContent = userPlatform.getClobData(rsStep,
                COLUMN_DISPLAY_PARAMS);
        Map<String, String> dependentValues = parseParamContent(dependentParamContent);
        step.setDisplayParams(dependentValues);

        String filterName = rsStep.getString(COLUMN_ANSWER_FILTER);
        String answerChecksum = rsStep.getString(AnswerFactory.COLUMN_ANSWER_CHECKSUM);

        try {
            // load Answer
            AnswerFactory answerFactory = wdkModel.getAnswerFactory();
            Answer answer = answerFactory.getAnswer(user, answerChecksum);
            step.setAnswer(answer);

            String questionName = answer.getQuestionName();
            Question question = wdkModel.getQuestion(questionName);
            RecordClass recordClass = question.getRecordClass();
            AnswerFilterInstance filter = (filterName == null) ? null
                    : recordClass.getFilter(filterName);
            int startIndex = 1;
            int endIndex = user.getItemsPerPage();
            Map<String, Boolean> sortingMap = user.getSortingAttributes(questionName);
            AnswerValue answerValue = question.makeAnswerValue(user,
                    dependentValues, startIndex, endIndex, sortingMap, filter);
            answerValue.setSumaryAttributes(user.getSummaryAttributes(questionName));
            answer.setAnswerValue(answerValue);
            answerValue.setAnswer(answer);

            step.setValid(true);
        } catch (Exception ex) {
            step.setValid(false);
        }
        return step;
    }

    private void updateStepTree(User user, Step step)
            throws NoSuchAlgorithmException, WdkUserException,
            WdkModelException, JSONException, SQLException {
        AnswerValue answerValue = step.getAnswer().getAnswerValue();
        Question question = answerValue.getQuestion();
        Map<String, String> displayParams = step.getDisplayParams();

        Query query = answerValue.getIdsQueryInstance().getQuery();
        boolean isBoolean = query.isBoolean();
        boolean isTransform = query.isTransform();
        int leftStepId = 0;
        int rightStepId = 0;
        String customName;
        if (isBoolean) {
            // boolean result, set the left and right step ids accordingly, and
            // set the constructed boolean expression to custom name.
            BooleanQuery booleanQuery = (BooleanQuery) query;

            AnswerParam leftParam = booleanQuery.getLeftOperandParam();
            String leftKey = displayParams.get(leftParam.getName());
            String leftStepKey = leftKey.substring(leftKey.indexOf(":") + 1);
            leftStepId = Integer.parseInt(leftStepKey);

            AnswerParam rightParam = booleanQuery.getRightOperandParam();
            String rightKey = displayParams.get(rightParam.getName());
            String rightStepKey = rightKey.substring(rightKey.indexOf(":") + 1);
            rightStepId = Integer.parseInt(rightStepKey);

            StringParam operatorParam = booleanQuery.getOperatorParam();
            String operator = displayParams.get(operatorParam.getName());

            customName = leftStepId + " " + operator + " " + rightKey;
        } else if (isTransform) {
            // transform result, set the left step id only, and combine the step
            // id into custom name.
            // ASSUMPTION: we can only handle one answerParam transforms.
            AnswerParam answerParam = null;
            for (Param param : question.getParams()) {
                if (param instanceof AnswerParam) {
                    answerParam = (AnswerParam) param;
                    break;
                }
            }
            String combinedKey = displayParams.get(answerParam.getName());
            String stepKey = combinedKey.substring(combinedKey.indexOf(":") + 1);
            leftStepId = Integer.parseInt(stepKey);

            customName = question.getShortDisplayName();
        } else customName = step.getBaseCustomName();

        // construct the update sql
        StringBuffer sql = new StringBuffer("UPDATE ");
        sql.append(userSchema).append(TABLE_STEP).append(" SET ");
        sql.append(COLUMN_CUSTOM_NAME).append(" = ? ");
        if (isBoolean || isTransform) {
            sql.append(", ").append(COLUMN_LEFT_CHILD_ID);
            sql.append(" = ").append(leftStepId);
            if (isBoolean) {
                sql.append(", ").append(COLUMN_RIGHT_CHILD_ID);
                sql.append(" = ").append(rightStepId);
            }
        }
        sql.append(" WHERE ").append(COLUMN_STEP_INTERNAL_ID);
        sql.append(" = ").append(step.getInternalId());

        step.setCustomName(customName);
        PreparedStatement psUpdateStepTree = null;
        try {
            psUpdateStepTree = SqlUtils.getPreparedStatement(dataSource,
                    sql.toString());
            psUpdateStepTree.setString(1, customName);
            psUpdateStepTree.executeUpdate();
        } finally {
            SqlUtils.closeStatement(psUpdateStepTree);
        }
    }

    /**
     * This method updates the custom name, the time stamp of last running,
     * isDeleted, isCollapsible, and collapsed name
     * 
     * @param user
     * @param step
     * @throws WdkUserException
     * @throws SQLException
     * @throws JSONException
     * @throws WdkModelException
     * @throws NoSuchAlgorithmException
     */
    void updateStep(User user, Step step, boolean updateTime)
            throws WdkUserException, SQLException, NoSuchAlgorithmException,
            WdkModelException, JSONException {
        // TEST
        // update custom name
        Date lastRunTime = (updateTime) ? new Date() : step.getLastRunTime();
        int estimateSize = step.getAnswer().getAnswerValue().getResultSize();
        PreparedStatement psStep = null;
        try {
            psStep = SqlUtils.getPreparedStatement(dataSource, "UPDATE "
                    + userSchema + TABLE_STEP + " SET " + COLUMN_CUSTOM_NAME
                    + " = ?, " + COLUMN_LAST_RUN_TIME + " = ?, "
                    + COLUMN_IS_DELETED + " = ?, " + COLUMN_IS_COLLAPSIBLE
                    + " = ?, " + COLUMN_COLLAPSED_NAME + " = ?, "
                    + COLUMN_ESTIMATE_SIZE + " = ? WHERE "
                    + COLUMN_STEP_INTERNAL_ID + " = ?");
            psStep.setString(1, step.getBaseCustomName());
            psStep.setTimestamp(2, new Timestamp(lastRunTime.getTime()));
            psStep.setBoolean(3, step.isDeleted());
            psStep.setBoolean(4, step.isCollapsible());
            psStep.setString(5, step.getCollapsedName());
            psStep.setInt(6, estimateSize);
            psStep.setInt(7, step.getInternalId());
            int result = psStep.executeUpdate();
            if (result == 0)
                throw new WdkUserException("The Step #" + step.getDisplayId()
                        + " of user " + user.getEmail() + " cannot be found.");

            // update the last run stamp
            step.setLastRunTime(lastRunTime);
            step.setEstimateSize(estimateSize);

            // update dependencies
            updateStepTree(user, step);
        } finally {
            SqlUtils.closeStatement(psStep);
        }
    }

    Map<Integer, Strategy> loadStrategies(User user,
            Map<Integer, Strategy> invalidStrategies) throws WdkUserException,
            WdkModelException, JSONException, SQLException {
        Map<Integer, Strategy> userStrategies = new LinkedHashMap<Integer, Strategy>();

        PreparedStatement psStrategyIds = null;
        ResultSet rsStrategyIds = null;

        try {
            psStrategyIds = SqlUtils.getPreparedStatement(dataSource,
                    "SELECT s." + COLUMN_DISPLAY_ID + " FROM " + userSchema
                            + TABLE_STRATEGY + " s, " + userSchema + TABLE_STEP
                            + " h WHERE s." + UserFactory.COLUMN_USER_ID
                            + " = ? AND s." + COLUMN_PROJECT_ID + " = ? AND s."
                            + COLUMN_IS_DELETED + " = ? AND h."
                            + UserFactory.COLUMN_USER_ID + " = s."
                            + UserFactory.COLUMN_USER_ID + " AND h."
                            + COLUMN_DISPLAY_ID + " = s." + COLUMN_ROOT_STEP_ID
                            + " ORDER BY h." + COLUMN_LAST_RUN_TIME + " DESC");
            psStrategyIds.setInt(1, user.getUserId());
            psStrategyIds.setString(2, wdkModel.getProjectId());
            psStrategyIds.setBoolean(3, false);
            rsStrategyIds = psStrategyIds.executeQuery();

            Strategy strategy;
            int strategyId;
            while (rsStrategyIds.next()) {
                strategyId = rsStrategyIds.getInt(COLUMN_DISPLAY_ID);
                strategy = loadStrategy(user, strategyId);
                userStrategies.put(strategyId, strategy);
            }

            return userStrategies;
        } finally {
            SqlUtils.closeStatement(psStrategyIds);
            SqlUtils.closeResultSet(rsStrategyIds);
        }
    }

    Strategy importStrategy(User user, Strategy oldStrategy)
            throws WdkUserException, WdkModelException, SQLException,
            NoSuchAlgorithmException, JSONException {
        logger.debug("import strategy #" + oldStrategy.getInternalId()
                + "(internal) to user #" + user.getUserId());

        Step oldRootStep = oldStrategy.getLatestStep();
        ResultSet rsNames = null;
        try {
            // get the existing names
            PreparedStatement psNames = SqlUtils.getPreparedStatement(
                    dataSource, "SELECT " + COLUMN_NAME + " FROM " + userSchema
                            + TABLE_STRATEGY + " WHERE "
                            + UserFactory.COLUMN_USER_ID + " = ? AND "
                            + COLUMN_PROJECT_ID + " = ?");
            psNames.setInt(1, user.getUserId());
            psNames.setString(2, wdkModel.getProjectId());
            rsNames = psNames.executeQuery();

            Set<String> names = new LinkedHashSet<String>();
            while (rsNames.next())
                names.add(rsNames.getString(COLUMN_NAME));

            String name = oldStrategy.getName();
            Pattern pattern = Pattern.compile("(.+?)\\((\\d+)\\)");
            while (names.contains(name)) {
                Matcher matcher = pattern.matcher(name);
                if (matcher.matches()) {
                    int number = Integer.parseInt(matcher.group(2));
                    name = matcher.group(1).trim();
                    name += " (" + (++number) + ")";
                } else { // the initial name, no tailing serial number
                    name += " (2)";
                }
            }

            // If user does not already have a copy of this strategy, need to
            // look up the answers recursively, construct step objects.
            Step latestStep = importStep(user, oldRootStep);
            // Need to create strategy & then load it so that all AnswerValues
            // are created properly
            Strategy strategy = createStrategy(user, latestStep, name, null,
                    oldStrategy.getIsSaved());
            return loadStrategy(user, strategy.getDisplayId());
        } finally {
            SqlUtils.closeResultSet(rsNames);
        }
    }

    Step importStep(User newUser, Step oldStep) throws WdkUserException,
            WdkModelException, SQLException, NoSuchAlgorithmException,
            JSONException {
        User oldUser = oldStep.getUser();

        // Is this answer a boolean? Import depended steps first.
        AnswerValue answerValue = oldStep.getAnswer().getAnswerValue();
        Question question = answerValue.getQuestion();
        AnswerFilterInstance filter = answerValue.getFilter();

        Map<String, Param> params = question.getParamMap();

        Map<String, String> paramValues = oldStep.getDisplayParams();
        for (String paramName : paramValues.keySet()) {
            Param param = params.get(paramName);
            String paramValue = paramValues.get(paramName);

            if (param instanceof AnswerParam) {
                int oldStepId = Integer.parseInt(paramValue);
                Step oldChildStep = oldUser.getStep(oldStepId);
                Step newChildStep = importStep(newUser, oldChildStep);
                paramValue = Integer.toString(newChildStep.getDisplayId());
            } else if (param instanceof DatasetParam) {
                int oldUserDatasetId = Integer.parseInt(paramValue);
                Dataset oldDataset = oldUser.getDataset(oldUserDatasetId);
                Dataset newDataset = newUser.getDataset(oldDataset.getChecksum());
                paramValue = Integer.toString(newDataset.getUserDatasetId());
            }
            paramValues.put(paramName, paramValue);
        }

        int startIndex = answerValue.getStartIndex();
        int endIndex = answerValue.getEndIndex();
        boolean deleted = oldStep.isDeleted();
        Step newStep = newUser.createStep(question, paramValues, filter,
                startIndex, endIndex, deleted);
        return newStep;
    }

    Strategy loadStrategy(User user, int displayId) throws WdkUserException,
            WdkModelException, JSONException, SQLException {
        String userIdColumn = UserFactory.COLUMN_USER_ID;

        PreparedStatement psStrategy = null;
        PreparedStatement psAnswerTree = null;
        ResultSet rsStrategy = null;
        ResultSet rsAnswerTree = null;
        try {
            psStrategy = SqlUtils.getPreparedStatement(dataSource, "SELECT "
                    + COLUMN_STRATEGY_INTERNAL_ID + ", " + COLUMN_NAME + ", "
                    + COLUMN_IS_SAVED + ", " + COLUMN_ROOT_STEP_ID + ", "
                    + COLUMN_CREATE_TIME + ", " + COLUMN_SAVED_NAME + " FROM "
                    + userSchema + TABLE_STRATEGY + " WHERE " + userIdColumn
                    + " = ? AND " + COLUMN_DISPLAY_ID + " = ? AND "
                    + COLUMN_PROJECT_ID + " = ? AND " + COLUMN_IS_DELETED
                    + " = ?");
            psStrategy.setInt(1, user.getUserId());
            psStrategy.setInt(2, displayId);
            psStrategy.setString(3, wdkModel.getProjectId());
            psStrategy.setBoolean(4, false);
            rsStrategy = psStrategy.executeQuery();
            if (!rsStrategy.next()) {
                throw new WdkUserException("The strategy " + displayId
                        + " does not exist " + "for user " + user.getEmail());
            }

            int internalId = rsStrategy.getInt(COLUMN_STRATEGY_INTERNAL_ID);

            Strategy strategy = new Strategy(this, user, displayId, internalId,
                    rsStrategy.getString(COLUMN_NAME));
            strategy.setIsSaved(rsStrategy.getBoolean(COLUMN_IS_SAVED));
            strategy.setCreatedTime(rsStrategy.getTimestamp(COLUMN_CREATE_TIME));
            strategy.setSavedName(rsStrategy.getString(COLUMN_SAVED_NAME));
            // Set saved name, if any
            /*
             * if
             * (!strategy.getName().matches("^New Strategy(\\([\\d]+\\))?\\*$"))
             * { // System.out.println("Name does not match: " + //
             * strategy.getName()); // Remove any (and everything after it) from
             * name, set as // saved name
             * strategy.setSavedName(strategy.getName().replaceAll(
             * "(\\([\\d]+\\))?\\*$", "")); }
             */

            // select parent_id, child_id, child step data FROM
            // step_tree JOIN step WHERE parent_id = latest parent
            // for each row returned, create Step, add Step to strategy, put
            // child_id on parent stack
            // Now add step_id to a stack, and go into while loop
            int currentStepId = rsStrategy.getInt(COLUMN_ROOT_STEP_ID);
            Integer currentStepIdObj = new Integer(currentStepId);
            Step currentStep = loadStep(user, currentStepId);

            strategy.addStep(currentStep);

            Stack<Integer> answerTree = new Stack<Integer>();
            answerTree.push(currentStepIdObj);

            HashMap<Integer, Step> steps = new HashMap<Integer, Step>();
            steps.put(currentStepIdObj, currentStep);

            Integer parentAnswerId;
            Step parentStep;

            psAnswerTree = SqlUtils.getPreparedStatement(dataSource, "SELECT "
                    + COLUMN_LEFT_CHILD_ID + ", " + COLUMN_RIGHT_CHILD_ID
                    + " FROM " + userSchema + TABLE_STEP + " WHERE "
                    + userIdColumn + " = ? AND " + COLUMN_DISPLAY_ID + " = ?");

            while (!answerTree.empty()) {
                parentAnswerId = answerTree.pop();

                psAnswerTree.setInt(1, user.getUserId());
                psAnswerTree.setInt(2, parentAnswerId.intValue());

                rsAnswerTree = psAnswerTree.executeQuery();

                if (rsAnswerTree.next()) {
                    parentStep = steps.get(parentAnswerId);

                    // left child
                    currentStepId = rsAnswerTree.getInt(COLUMN_LEFT_CHILD_ID);
                    if (currentStepId >= 1) {
                        currentStep = loadStep(user, currentStepId);
                        answerTree.push(currentStepId);
                        steps.put(currentStepId, currentStep);

                        parentStep.setPreviousStep(currentStep);
                        currentStep.setNextStep(parentStep);
                    }
                    // right child
                    currentStepId = rsAnswerTree.getInt(COLUMN_RIGHT_CHILD_ID);
                    if (currentStepId >= 1) {
                        currentStep = loadStep(user, currentStepId);
                        answerTree.push(currentStepId);
                        steps.put(currentStepId, currentStep);

                        parentStep.setChildStep(currentStep);
                        currentStep.setParentStep(parentStep);
                    }
                }
            }

            return strategy;
        } finally {
            SqlUtils.closeStatement(psStrategy);
            SqlUtils.closeStatement(psAnswerTree);
            SqlUtils.closeResultSet(rsStrategy);
            SqlUtils.closeResultSet(rsAnswerTree);
        }
    }

    // This function only updates the strategies table
    void updateStrategy(User user, Strategy strategy, boolean overwrite)
            throws WdkUserException, WdkModelException, SQLException,
            JSONException {
        // update strategy name, saved, step_id
        PreparedStatement psStrategy = null;
        ResultSet rsStrategy = null;

        int userId = user.getUserId();

        String userIdColumn = UserFactory.COLUMN_USER_ID;
        try {
            if (overwrite) {
                // If we're overwriting, need to look up saved strategy id by
                // name
                // (only if the saved strategy is not the one we're updating,
                // i.e.
                // the saved strategy id != this strategy id)
                PreparedStatement psCheck = SqlUtils.getPreparedStatement(
                        dataSource, "SELECT " + COLUMN_STRATEGY_INTERNAL_ID
                                + ", " + COLUMN_DISPLAY_ID + " FROM "
                                + userSchema + TABLE_STRATEGY + " WHERE "
                                + userIdColumn + " = ? AND "
                                + COLUMN_PROJECT_ID + " = ? AND " + COLUMN_NAME
                                + " = ? AND " + COLUMN_IS_SAVED + " = ? AND "
                                + COLUMN_IS_DELETED + " = ? AND "
                                + COLUMN_DISPLAY_ID + " <> ?");
                psCheck.setInt(1, userId);
                psCheck.setString(2, wdkModel.getProjectId());
                psCheck.setString(3, strategy.getName());
                psCheck.setBoolean(4, true);
                psCheck.setBoolean(5, false);
                psCheck.setInt(6, strategy.getDisplayId());
                rsStrategy = psCheck.executeQuery();

                // If there's already a saved strategy with this strategy's
                // name,
                // we need to write the new saved strategy & mark the old
                // saved strategy as deleted
                if (rsStrategy.next()) {
                    int idToDelete = rsStrategy.getInt(COLUMN_DISPLAY_ID);
                    strategy.setIsSaved(true);
                    strategy.setSavedName(strategy.getName());
                    user.deleteStrategy(idToDelete);
                }
            } else if (strategy.getIsSaved()) {
                // If we're not overwriting a saved strategy, then we're
                // modifying
                // it. We need to get an unsaved copy to modify. Generate
                // unsaved name
                PreparedStatement psCheck = SqlUtils.getPreparedStatement(
                        dataSource, "SELECT 1, " + COLUMN_NAME + " FROM "
                                + userSchema + TABLE_STRATEGY + " WHERE "
                                + userIdColumn + " = ? AND "
                                + COLUMN_PROJECT_ID + " = ? AND " + COLUMN_NAME
                                + " = ? AND " + COLUMN_IS_SAVED + "= ? AND "
                                + COLUMN_IS_DELETED + "= ? UNION "
                                + "SELECT 2, " + COLUMN_NAME + " FROM "
                                + userSchema + TABLE_STRATEGY + " WHERE "
                                + userIdColumn + " = ? AND "
                                + COLUMN_PROJECT_ID + " = ? AND " + COLUMN_NAME
                                + " LIKE ? AND " + COLUMN_IS_SAVED + "= ? AND "
                                + COLUMN_IS_DELETED + " = ? ORDER BY 1, "
                                + COLUMN_NAME);
                psCheck.setInt(1, userId);
                psCheck.setString(2, wdkModel.getProjectId());
                psCheck.setString(3, strategy.getName());
                psCheck.setBoolean(4, false);
                psCheck.setBoolean(5, false);
                psCheck.setInt(6, userId);
                psCheck.setString(7, wdkModel.getProjectId());
                psCheck.setString(8, strategy.getName() + "(%)");
                psCheck.setBoolean(9, false);
                psCheck.setBoolean(10, false);
                rsStrategy = psCheck.executeQuery();
                logger.info("savedName: " + strategy.getSavedName());

                String append = "";
                String name;
                int current = 1;
                while (rsStrategy.next()) {
                    name = rsStrategy.getString(COLUMN_NAME);
                    logger.info("Name " + current + ": " + name);
                    if (name.equals(strategy.getSavedName() + append)) {
                        append = "(" + ++current + ")";
                    }
                }

                Strategy newStrat = createStrategy(user,
                        strategy.getLatestStep(), strategy.getName() + append,
                        strategy.getName(), false);
                strategy.setName(newStrat.getName());
                strategy.setSavedName(newStrat.getSavedName());
                strategy.setDisplayId(newStrat.getDisplayId());
                strategy.setInternalId(newStrat.getInternalId());
                strategy.setIsSaved(false);
            }

            psStrategy = SqlUtils.getPreparedStatement(dataSource, "UPDATE "
                    + userSchema + TABLE_STRATEGY + " SET " + COLUMN_NAME
                    + " = ?, " + COLUMN_ROOT_STEP_ID + " = ?, "
                    + COLUMN_SAVED_NAME + " = ?, " + COLUMN_IS_SAVED
                    + " = ? WHERE " + COLUMN_STRATEGY_INTERNAL_ID + " = ?");
            psStrategy.setString(1, strategy.getName());
            psStrategy.setInt(2, strategy.getLatestStep().getDisplayId());
            psStrategy.setString(3, strategy.getSavedName());
            psStrategy.setBoolean(4, strategy.getIsSaved());
            psStrategy.setInt(5, strategy.getInternalId());
            int result = psStrategy.executeUpdate();

            if (result == 0)
                throw new WdkUserException("The strategy #"
                        + strategy.getDisplayId() + " of user "
                        + user.getEmail() + " cannot be found.");
        } finally {
            SqlUtils.closeStatement(psStrategy);
            SqlUtils.closeResultSet(rsStrategy);
        }

    }

    // Note: this function only adds the necessary row in strategies; updating
    // of answers
    // and steps tables is handled in other functions. Once the Step
    // object exists, all of this data is already in the db.
    Strategy createStrategy(User user, Step root, String name,
            String savedName, boolean saved) throws SQLException,
            WdkUserException, WdkModelException, JSONException {
        int userId = user.getUserId();

        String userIdColumn = UserFactory.COLUMN_USER_ID;
        ResultSet rsCheckName = null;
        PreparedStatement psCheckName;

        try {
            // If name is not null, check if strategy exists
            if (name != null) {
                psCheckName = SqlUtils.getPreparedStatement(dataSource,
                        "SELECT " + COLUMN_DISPLAY_ID + " FROM " + userSchema
                                + TABLE_STRATEGY + " WHERE " + userIdColumn
                                + " = ? AND " + COLUMN_PROJECT_ID + " = ? AND "
                                + COLUMN_NAME + " = ? AND " + COLUMN_IS_SAVED
                                + "= ? AND " + COLUMN_IS_DELETED + "= ?");
                psCheckName.setInt(1, userId);
                psCheckName.setString(2, wdkModel.getProjectId());
                psCheckName.setString(3, name);
                psCheckName.setBoolean(4, saved);
                psCheckName.setBoolean(5, false);
                rsCheckName = psCheckName.executeQuery();

                if (rsCheckName.next())
                    return loadStrategy(user,
                            rsCheckName.getInt(COLUMN_DISPLAY_ID));
            } else {// otherwise, generate default name
                psCheckName = SqlUtils.getPreparedStatement(dataSource,
                        "SELECT 1, " + COLUMN_NAME + " FROM " + userSchema
                                + TABLE_STRATEGY + " WHERE " + userIdColumn
                                + " = ? AND " + COLUMN_PROJECT_ID + " = ? AND "
                                + COLUMN_IS_DELETED + " = ? AND " + COLUMN_NAME
                                + " = 'New Strategy'" + " UNION "
                                + "SELECT 2, " + COLUMN_NAME + " FROM "
                                + userSchema + TABLE_STRATEGY + " WHERE "
                                + userIdColumn + " = ? AND "
                                + COLUMN_PROJECT_ID + " = ? AND "
                                + COLUMN_IS_DELETED + "= ? AND " + COLUMN_NAME
                                + " LIKE 'New Strategy(%)'" + "ORDER BY 1, "
                                + COLUMN_NAME);
                psCheckName.setInt(1, userId);
                psCheckName.setString(2, wdkModel.getProjectId());
                psCheckName.setBoolean(3, false);
                psCheckName.setInt(4, userId);
                psCheckName.setString(5, wdkModel.getProjectId());
                psCheckName.setBoolean(6, false);
                rsCheckName = psCheckName.executeQuery();

                int current = 1;
                String append = "";
                while (rsCheckName.next()) {
                    name = rsCheckName.getString(COLUMN_NAME);
                    logger.info("Name: " + name);
                    if (name.equals("New Strategy" + append)) {
                        append = "(" + ++current + ")";
                    }
                }
                name = "New Strategy" + append;
            }
        } finally {
            SqlUtils.closeResultSet(rsCheckName);
        }

        int displayId;
        PreparedStatement psMax = null;
        PreparedStatement psStrategy = null;
        ResultSet rsMax = null;
        Connection connection = dataSource.getConnection();

        int strategyId = userPlatform.getNextId(userSchema, TABLE_STRATEGY);
        try {
            synchronized (connection) {
                connection.setAutoCommit(false);

                // get the current max strategy id
                psMax = connection.prepareStatement("SELECT max("
                        + COLUMN_DISPLAY_ID + ") max_id FROM " + userSchema
                        + TABLE_STRATEGY + " WHERE " + userIdColumn
                        + " = ? AND " + COLUMN_PROJECT_ID + " = ?");
                psMax.setInt(1, userId);
                psMax.setString(2, wdkModel.getProjectId());
                rsMax = psMax.executeQuery();

                if (rsMax.next()) displayId = rsMax.getInt("max_id") + 1;
                else displayId = 1;

                // insert the row into strategies
                psStrategy = SqlUtils.getPreparedStatement(dataSource,
                        "INSERT INTO " + userSchema + TABLE_STRATEGY + " ("
                                + COLUMN_DISPLAY_ID + ", "
                                + COLUMN_STRATEGY_INTERNAL_ID + ", "
                                + userIdColumn + ", " + COLUMN_ROOT_STEP_ID
                                + ", " + COLUMN_IS_SAVED + ", " + COLUMN_NAME
                                + ", " + COLUMN_SAVED_NAME + ", "
                                + COLUMN_PROJECT_ID + ", " + COLUMN_IS_DELETED
                                + ") VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)");
                psStrategy.setInt(1, displayId);
                psStrategy.setInt(2, strategyId);
                psStrategy.setInt(3, userId);
                psStrategy.setInt(4, root.getDisplayId());
                psStrategy.setBoolean(5, saved);
                psStrategy.setString(6, name);
                psStrategy.setString(7, savedName);
                psStrategy.setString(8, wdkModel.getProjectId());
                psStrategy.setBoolean(9, false);
                psStrategy.executeUpdate();

                connection.commit();
            }
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
            SqlUtils.closeStatement(psStrategy);
            SqlUtils.closeResultSet(rsMax);
        }

        return loadStrategy(user, displayId);
    }

    int getStrategyCount(User user) throws WdkUserException, SQLException {
        ResultSet rsStrategy = null;
        try {
            PreparedStatement psStrategy = SqlUtils.getPreparedStatement(
                    dataSource, "SELECT count(*) AS num FROM " + userSchema
                            + TABLE_STRATEGY + " WHERE "
                            + UserFactory.COLUMN_USER_ID + " = ? AND "
                            + COLUMN_IS_DELETED + " = ? AND "
                            + COLUMN_PROJECT_ID + " = ? ");
            psStrategy.setInt(1, user.getUserId());
            psStrategy.setBoolean(2, false);
            psStrategy.setString(3, wdkModel.getProjectId());
            rsStrategy = psStrategy.executeQuery();
            rsStrategy.next();
            return rsStrategy.getInt("num");
        } finally {
            SqlUtils.closeResultSet(rsStrategy);
        }
    }

    private String getParamContent(Map<String, String> params)
            throws JSONException {
        JSONObject json = new JSONObject();
        for (String paramName : params.keySet()) {
            json.put(paramName, params.get(paramName));
        }
        return json.toString();
    }

    private Map<String, String> parseParamContent(String paramContent)
            throws JSONException {
        Map<String, String> params = new LinkedHashMap<String, String>();
        if (paramContent != null && paramContent.length() > 0) {
            JSONObject json = new JSONObject(paramContent);
            String[] paramNames = JSONObject.getNames(json);
            if (paramNames != null) {
                for (String paramName : paramNames) {
                    String paramValue = json.getString(paramName);
                    params.put(paramName, paramValue);
                }
            }
        }
        return params;
    }

    boolean checkNameExists(Strategy strategy, String name, boolean saved)
            throws SQLException {
        ResultSet rsCheckName = null;
        try {
            PreparedStatement psCheckName = SqlUtils.getPreparedStatement(
                    dataSource, "SELECT display_id FROM " + userSchema
                            + TABLE_STRATEGY + " WHERE "
                            + UserFactory.COLUMN_USER_ID + " = ? AND "
                            + COLUMN_PROJECT_ID + " = ? AND " + COLUMN_NAME
                            + " = ? AND " + COLUMN_IS_SAVED + " = ? AND "
                            + COLUMN_IS_DELETED + " = ? AND "
                            + COLUMN_DISPLAY_ID + " <> ?");
            psCheckName.setInt(1, strategy.getUser().getUserId());
            psCheckName.setString(2, wdkModel.getProjectId());
            psCheckName.setString(3, name);
            psCheckName.setBoolean(4, (saved || strategy.getIsSaved()));
            psCheckName.setBoolean(5, false);
            psCheckName.setInt(6, strategy.getDisplayId());
            rsCheckName = psCheckName.executeQuery();

            if (rsCheckName.next()) return true;

            return false;
        } finally {
            SqlUtils.closeResultSet(rsCheckName);
        }
    }
}
