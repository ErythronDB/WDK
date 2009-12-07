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
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;

import javax.sql.DataSource;

import org.apache.log4j.Logger;
import org.gusdb.wdk.model.ModelConfigUserDB;
import org.gusdb.wdk.model.RecordClass;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.dbms.SqlUtils;
import org.json.JSONArray;

/**
 * @author xingao
 * 
 */
public class DatasetFactory {

    public static final String TABLE_DATASET_VALUE = "dataset_values2";
    public static final String TABLE_DATASET_INDEX = "dataset_indices2";
    public static final String TABLE_USER_DATASET = "user_datasets2";

    public static final String COLUMN_DATASET_ID = "dataset_id";
    public static final String COLUMN_DATASET_VALUE = "dataset_value";
    public static final String COLUMN_DATASET_CHECKSUM = "dataset_checksum";
    public static final String COLUMN_USER_DATASET_ID = "user_dataset_id";
    private static final String COLUMN_DATASET_SIZE = "dataset_size";
    private static final String COLUMN_SUMMARY = "summary";
    private static final String COLUMN_CREATE_TIME = "create_time";
    private static final String COLUMN_UPLOAD_FILE = "upload_file";
    private static final String COLUMN_RECORD_CLASS = "record_class";

    private static final String REGEX_COLUMN_DIVIDER = "[ ,\t]+";
    private static final String REGEX_ROW_DIVIDER = "[\n;]";

    private static Logger logger = Logger.getLogger(DatasetFactory.class);

    private WdkModel wdkModel;
    private DBPlatform userPlatform;
    private DataSource dataSource;
    private String userSchema;
    private String wdkSchema;

    public DatasetFactory(WdkModel wdkModel) {
        this.wdkModel = wdkModel;
        this.userPlatform = this.wdkModel.getUserPlatform();
        this.dataSource = userPlatform.getDataSource();

        ModelConfigUserDB userDB = wdkModel.getModelConfig().getUserDB();
        this.userSchema = userDB.getUserSchema();
        this.wdkSchema = userDB.getWdkEngineSchema();
    }

    public Dataset getDataset(User user, RecordClass recordClass,
            String uploadFile, String strValues)
            throws NoSuchAlgorithmException, WdkModelException,
            WdkUserException, SQLException {
        List<String[]> values = parseValues(recordClass, strValues);
        return getDataset(user, recordClass, uploadFile, values);
    }

    public Dataset getDataset(User user, RecordClass recordClass,
            String uploadFile, List<String[]> values)
            throws NoSuchAlgorithmException, WdkModelException,
            WdkUserException, SQLException {
        if (values.size() == 0)
            throw new WdkDatasetException("The dataset is empty. User #"
                    + user.getUserId());
        String checksum = getChecksum(values);
        Connection connection = userPlatform.getDataSource().getConnection();
        try {
            Dataset dataset;
            connection.setAutoCommit(false);

            // check if dataset exists
            try {
                // get dataset id, catch WdkModelException if it doesn't exist
                int datasetId = getDatasetId(connection, checksum);
                dataset = new Dataset(this, datasetId);
                loadDatasetIndex(connection, dataset);
            } catch (WdkModelException ex) {
                logger.debug("Creating dataset for user #" + user.getUserId());

                // doesn't exist, create one
                dataset = insertDatasetIndex(recordClass, connection, checksum,
                        values);
                dataset.setChecksum(checksum);

                // and save the values
                insertDatasetValues(recordClass, connection, dataset, values);
            }
            dataset.setUser(user);

            // check if user dataset exists
            try {
                int userDatasetId = getUserDatasetId(connection, user,
                        dataset.getDatasetId());
                logger.debug("user dataset exist: " + userDatasetId);
                dataset.setUserDatasetId(userDatasetId);
                loadUserDataset(connection, dataset);
            } catch (WdkModelException ex) {
                // user-dataset doesn't exist, insert it
                dataset.setUploadFile(uploadFile);
                insertUserDataset(connection, dataset);
            }
            connection.commit();
            return dataset;
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            if (connection != null) {
                connection.setAutoCommit(true);
                connection.close();
            }
        }
    }

    /**
     * Get dataset by userDatasetId;
     * 
     * @param user
     * @param userDatasetId
     * @return
     * @throws SQLException
     * @throws WdkModelException
     *             throws if the userDatasetId doesn't exist or doesn't belong
     *             to the given user.
     * @throws WdkUserException
     */
    public Dataset getDataset(User user, int userDatasetId)
            throws SQLException, WdkModelException, WdkUserException {
        StringBuffer sql = new StringBuffer("SELECT ");
        sql.append(COLUMN_DATASET_ID);
        sql.append(" FROM ").append(userSchema).append(TABLE_USER_DATASET);
        sql.append(" WHERE ").append(Utilities.COLUMN_USER_ID);
        sql.append(" = ").append(user.getUserId());
        sql.append(" AND ").append(COLUMN_USER_DATASET_ID);
        sql.append(" = ").append(userDatasetId);

        DataSource dataSource = userPlatform.getDataSource();
        Object result = SqlUtils.executeScalar(wdkModel, dataSource,
                sql.toString());
        int datasetId = Integer.parseInt(result.toString());

        Dataset dataset = new Dataset(this, datasetId);
        dataset.setUser(user);
        dataset.setUserDatasetId(userDatasetId);

        Connection connection = dataSource.getConnection();
        try {
            loadDatasetIndex(connection, dataset);
            loadUserDataset(connection, dataset);
        } finally {
            if (connection != null) connection.close();
        }
        return dataset;
    }

    /**
     * Get a dataset from checksum; if the dataset exists but userDataset
     * doesn't, a new user dataset will be created
     * 
     * @param user
     * @param datasetChecksum
     * @return
     * @throws SQLException
     * @throws WdkModelException
     *             throws if the dataset doesn't exist;
     * @throws WdkUserException
     */
    public Dataset getDataset(User user, String datasetChecksum)
            throws SQLException, WdkModelException, WdkUserException {
        // get dataset id
        StringBuffer sqlDatasetId = new StringBuffer("SELECT ");
        sqlDatasetId.append(COLUMN_DATASET_ID);
        sqlDatasetId.append(" FROM ").append(wdkSchema).append(
                TABLE_DATASET_INDEX);
        sqlDatasetId.append(" WHERE ").append(COLUMN_DATASET_CHECKSUM).append(
                " = ?");
        int datasetId;
        ResultSet resultSet = null;
        try {
            long start = System.currentTimeMillis();
            String sql = sqlDatasetId.toString();
            PreparedStatement psQuery = SqlUtils.getPreparedStatement(
                    dataSource, sql);
            psQuery.setString(1, datasetChecksum);
            resultSet = psQuery.executeQuery();
            SqlUtils.verifyTime(wdkModel, sql, start);
            if (!resultSet.next())
                throw new WdkModelException("The dataset with checksum '"
                        + datasetChecksum + "' doesn't exist.");
            datasetId = resultSet.getInt(COLUMN_DATASET_ID);
        } finally {
            SqlUtils.closeResultSet(resultSet);
        }

        // try to get a user dataset id
        Connection connection = dataSource.getConnection();
        try {
            connection.setAutoCommit(false);

            Dataset dataset = new Dataset(this, datasetId);
            dataset.setUser(user);
            loadDatasetIndex(connection, dataset);
            try {
                int userDatasetId = getUserDatasetId(connection, user,
                        datasetId);
                dataset.setUserDatasetId(userDatasetId);
                loadUserDataset(connection, dataset);
            } catch (WdkModelException ex) {
                // user data set doesn't exist
                dataset.setUploadFile("");
                insertUserDataset(connection, dataset);
            }
            return dataset;
        } catch (SQLException ex) {
            connection.rollback();
            throw ex;
        } finally {
            connection.setAutoCommit(true);
            connection.close();
        }
    }

    List<String[]> getDatasetValues(Dataset dataset) throws SQLException,
            WdkUserException, WdkModelException {
        String columnPrefx = Utilities.COLUMN_PK_PREFIX;
        int columnCount = Utilities.MAX_PK_COLUMN_COUNT;
        StringBuffer sql = new StringBuffer();
        for (int i = 1; i <= columnCount; i++) {
            if (sql.length() == 0) sql.append("SELECT ");
            else sql.append(", ");
            sql.append(columnPrefx + i);
        }
        sql.append(" FROM ").append(wdkSchema).append(TABLE_DATASET_VALUE);
        sql.append(" WHERE ").append(COLUMN_DATASET_ID);
        sql.append(" = ").append(dataset.getDatasetId());

        ResultSet resultSet = null;
        DataSource dataSource = userPlatform.getDataSource();
        try {
            resultSet = SqlUtils.executeQuery(wdkModel, dataSource,
                    sql.toString());
            List<String[]> values = new ArrayList<String[]>();
            while (resultSet.next()) {
                String[] columns = new String[columnCount];
                for (int i = 1; i <= columnCount; i++) {
                    columns[i - 1] = resultSet.getString(columnPrefx + i);
                }
                values.add(columns);
            }
            return values;
        } finally {
            SqlUtils.closeResultSet(resultSet);
        }

    }

    /**
     * @param connection
     * @param datasetChecksum
     * @return returns dataset Id.
     * @throws WdkModelException
     *             the dataset does not exist
     * @throws SQLException
     *             the database or query failure
     * @throws WdkUserException
     */
    private int getDatasetId(Connection connection, String datasetChecksum)
            throws SQLException, WdkModelException, WdkUserException {
        StringBuffer sql = new StringBuffer("SELECT ");
        sql.append(COLUMN_DATASET_ID);
        sql.append(" FROM ").append(wdkSchema).append(TABLE_DATASET_INDEX);
        sql.append(" WHERE ").append(COLUMN_DATASET_CHECKSUM);
        sql.append(" = '").append(datasetChecksum).append("'");

        Object result = SqlUtils.executeScalar(wdkModel, dataSource,
                sql.toString());
        return Integer.parseInt(result.toString());
    }

    /**
     * @param connection
     * @param datasetId
     * @return the user-dataset-id.
     * @throws WdkModelException
     *             the userDataset does not exist
     * @throws SQLException
     *             the database or query failure
     * @throws WdkUserException
     */
    public int getUserDatasetId(Connection connection, User user, int datasetId)
            throws SQLException, WdkModelException, WdkUserException {
        StringBuffer sql = new StringBuffer("SELECT ");
        sql.append(COLUMN_USER_DATASET_ID);
        sql.append(" FROM ").append(userSchema).append(TABLE_USER_DATASET);
        sql.append(" WHERE ").append(COLUMN_DATASET_ID).append(" = ").append(
                datasetId);
        sql.append(" AND ").append(Utilities.COLUMN_USER_ID).append(" = ").append(
                user.getUserId());

        Object result = SqlUtils.executeScalar(wdkModel, dataSource,
                sql.toString());
        return Integer.parseInt(result.toString());
    }

    private Dataset insertDatasetIndex(RecordClass recordClass,
            Connection connection, String checksum, List<String[]> values)
            throws SQLException, WdkModelException, WdkUserException {
        // get a new dataset id
        int datasetId = userPlatform.getNextId(wdkSchema, TABLE_DATASET_INDEX);
        Dataset dataset = new Dataset(this, datasetId);
        dataset.setChecksum(checksum);
        dataset.setRecordClass(recordClass);

        // set summary
        dataset.setSummary(values);

        StringBuffer sql = new StringBuffer("INSERT INTO ");
        sql.append(wdkSchema).append(TABLE_DATASET_INDEX).append(" (");
        sql.append(COLUMN_DATASET_ID).append(", ");
        sql.append(COLUMN_DATASET_CHECKSUM).append(", ");
        sql.append(COLUMN_DATASET_SIZE).append(", ");
        sql.append(COLUMN_RECORD_CLASS).append(", ");
        sql.append(COLUMN_SUMMARY).append(") VALUES (?, ?, ?, ?, ?)");
        PreparedStatement psInsert = connection.prepareStatement(sql.toString());
        try {
            psInsert.setInt(1, datasetId);
            psInsert.setString(2, checksum);
            psInsert.setInt(3, dataset.getSize());
            psInsert.setString(4, recordClass.getFullName());
            psInsert.setString(5, dataset.getSummary());
            psInsert.execute();
        } finally {
            if (psInsert != null) psInsert.close();
        }
        return dataset;
    }

    private void insertDatasetValues(RecordClass recordClass,
            Connection connection, Dataset dataset, List<String[]> values)
            throws SQLException {
        int columnCount = recordClass.getPrimaryKeyAttributeField().getColumnRefs().length;

        StringBuffer sql = new StringBuffer("INSERT INTO ");
        sql.append(wdkSchema).append(TABLE_DATASET_VALUE);
        sql.append(" (").append(COLUMN_DATASET_ID);
        for (int i = 1; i <= columnCount; i++) {
            sql.append(", ").append(Utilities.COLUMN_PK_PREFIX + i);
        }
        sql.append(") VALUES (?");
        for (int i = 1; i <= columnCount; i++) {
            sql.append(", ?");
        }
        sql.append(")");

        PreparedStatement psInsert = connection.prepareStatement(sql.toString());
        try {
            for (int i = 0; i < values.size(); i++) {
                String[] value = values.get(i);
                psInsert.setInt(1, dataset.getDatasetId());
                for (int j = 0; j < columnCount; j++) {
                    String val = (j < value.length) ? value[j] : null;
                    psInsert.setString(j + 2, val);
                }
                psInsert.addBatch();

                if ((i + 1) % 1000 == 0) psInsert.executeBatch();
            }
            if (values.size() % 1000 != 0) psInsert.executeBatch();
        } finally {
            if (psInsert != null) psInsert.close();
        }
    }

    private void insertUserDataset(Connection connection, Dataset dataset)
            throws SQLException, WdkModelException, WdkUserException {
        // get new user dataset id
        int userDatasetId = userPlatform.getNextId(userSchema,
                TABLE_USER_DATASET);

        logger.debug("Inserting new user dataset id: " + userDatasetId);
        dataset.setUserDatasetId(userDatasetId);
        dataset.setCreateTime(new Date());

        StringBuffer sql = new StringBuffer("INSERT INTO ");
        sql.append(userSchema).append(TABLE_USER_DATASET).append(" (");
        sql.append(COLUMN_USER_DATASET_ID).append(", ");
        sql.append(COLUMN_DATASET_ID).append(", ");
        sql.append(Utilities.COLUMN_USER_ID).append(", ");
        sql.append(COLUMN_CREATE_TIME).append(", ");
        sql.append(COLUMN_UPLOAD_FILE).append(") VALUES (?, ?, ?, ?, ?)");

        PreparedStatement psInsert = connection.prepareStatement(sql.toString());
        try {
            psInsert.setInt(1, userDatasetId);
            psInsert.setInt(2, dataset.getDatasetId());
            psInsert.setInt(3, dataset.getUser().getUserId());
            psInsert.setTimestamp(4, new Timestamp(
                    dataset.getCreateTime().getTime()));
            psInsert.setString(5, dataset.getUploadFile());
            psInsert.executeUpdate();
        } finally {
            if (psInsert != null) psInsert.close();
        }
    }

    private void loadDatasetIndex(Connection connection, Dataset dataset)
            throws SQLException, WdkModelException {
        StringBuffer sql = new StringBuffer("SELECT * ");
        sql.append(" FROM ").append(wdkSchema).append(TABLE_DATASET_INDEX);
        sql.append(" WHERE ").append(COLUMN_DATASET_ID).append(" = ");
        sql.append(dataset.getDatasetId());

        Statement stmt = connection.createStatement();
        ResultSet resultSet = stmt.executeQuery(sql.toString());
        try {
            if (!resultSet.next())
                throw new WdkModelException("The dataset ("
                        + dataset.getDatasetId() + ") does not exist.");
            dataset.setChecksum(resultSet.getString(COLUMN_DATASET_CHECKSUM));
            dataset.setSize(resultSet.getInt(COLUMN_DATASET_SIZE));
            dataset.setSummary(resultSet.getString(COLUMN_SUMMARY));

            String rcName = resultSet.getString(COLUMN_RECORD_CLASS);
            RecordClass recordClass = (RecordClass) wdkModel.resolveReference(rcName);
            dataset.setRecordClass(recordClass);
        } finally {
            try {
                if (resultSet != null) resultSet.close();
            } finally {
                if (stmt != null) stmt.close();
            }
        }
    }

    private void loadUserDataset(Connection connection, Dataset dataset)
            throws SQLException, WdkModelException {
        StringBuffer sql = new StringBuffer("SELECT ");
        sql.append(COLUMN_CREATE_TIME).append(", ").append(COLUMN_UPLOAD_FILE);
        sql.append(" FROM ").append(userSchema).append(TABLE_USER_DATASET);
        sql.append(" WHERE ").append(COLUMN_USER_DATASET_ID).append(" = ");
        sql.append(dataset.getUserDatasetId());

        Statement stmt = connection.createStatement();
        ResultSet resultSet = stmt.executeQuery(sql.toString());
        try {
            if (!resultSet.next())
                throw new WdkModelException("The userDataset ("
                        + dataset.getUserDatasetId() + ") does not exist.");
            dataset.setCreateTime(resultSet.getTimestamp(COLUMN_CREATE_TIME));
            dataset.setUploadFile(resultSet.getString(COLUMN_UPLOAD_FILE));
        } finally {
            try {
                if (resultSet != null) resultSet.close();
            } finally {
                if (stmt != null) stmt.close();
            }
        }
    }

    private String getChecksum(List<String[]> values)
            throws NoSuchAlgorithmException, WdkModelException {
        // sort the value list
        Collections.sort(values, new Comparator<String[]>() {
            public int compare(String[] o1, String[] o2) {
                int limit = Math.min(o1.length, o2.length);
                for (int i = 0; i < limit; i++) {
                    int result = o1[i].compareTo(o2[i]);
                    if (result != 0) return result;
                }
                return 0;
            }
        });
        JSONArray records = new JSONArray();
        for (String[] value : values) {
            JSONArray record = new JSONArray();
            for (String column : value) {
                record.put(column);
            }
            records.put(record);
        }
        return Utilities.encrypt(records.toString());
    }

    public List<String[]> parseValues(RecordClass recordClass, String strValue)
            throws WdkDatasetException {
        String[] rows = strValue.split(REGEX_ROW_DIVIDER);
        List<String[]> records = new ArrayList<String[]>();
        int length = recordClass.getPrimaryKeyAttributeField().getColumnRefs().length;
        for (String row : rows) {
            row = row.trim();
            if (row.length() == 0) continue;
            String[] columns = row.split(REGEX_COLUMN_DIVIDER);
            if (columns.length > length)
                throw new WdkDatasetException("The dataset raw "
                        + "value of recordClass '" + recordClass.getFullName()
                        + "' has more columns than expected: '" + row + "'");
            String[] record = new String[length];
            System.arraycopy(columns, 0, record, 0, columns.length);
            records.add(record);
        }
        return records;
    }
}
