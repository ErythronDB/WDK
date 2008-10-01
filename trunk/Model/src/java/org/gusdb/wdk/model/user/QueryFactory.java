package org.gusdb.wdk.model.user;

import java.security.NoSuchAlgorithmException;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

import javax.sql.DataSource;

import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.dbms.DBPlatform;
import org.gusdb.wdk.model.dbms.SqlUtils;

public class QueryFactory {

    private static final String TABLE_CLOB_VALUES = "clob_values";

    private static final String COLUMN_CLOB_CHECKSUM = "clob_checksum";
    private static final String COLUMN_CLOB_VALUE = "clob_value";

    // private static final Logger logger = Logger.getLogger( QueryFactory.class
    // );

    private String querySchema;
    private DBPlatform platform;

    public QueryFactory(DBPlatform platform, String querySchema) {
        this.platform = platform;
        this.querySchema = querySchema;
    }

    public String makeSummaryChecksum(String[] summaryAttributes)
            throws WdkModelException, WdkUserException,
            NoSuchAlgorithmException {
        // create checksum for config columns
        StringBuffer sb = new StringBuffer();
        for (String attribute : summaryAttributes) {
            if (sb.length() > 0) sb.append(", ");
            sb.append(attribute);
        }
        String summaryContent = sb.toString();
        String checksum = Utilities.encrypt(summaryContent);

        // check if the configuration exists
        DataSource dataSource = platform.getDataSource();
        PreparedStatement psInsert = null;
        try {
            if (null != getSummaryAttributes(checksum)) return checksum;

            // configuration not exists, add one
            psInsert = SqlUtils.getPreparedStatement(dataSource, "INSERT INTO"
                    + " " + querySchema + TABLE_CLOB_VALUES + " ("
                    + COLUMN_CLOB_CHECKSUM + ", " + COLUMN_CLOB_VALUE
                    + ") VALUES (?, ?)");
            psInsert.setString(1, checksum);
            psInsert.setString(2, summaryContent);
            psInsert.execute();

            return checksum;
        } catch (SQLException ex) {
            throw new WdkUserException(ex);
        } finally {
            try {
                SqlUtils.closeStatement(psInsert);
            } catch (SQLException ex) {
                throw new WdkUserException(ex);
            }
        }
    }

    public String[] getSummaryAttributes(String summaryChecksum)
            throws WdkUserException {
        DataSource dataSource = platform.getDataSource();
        ResultSet rsSelect = null;
        try {
            PreparedStatement psSelect = SqlUtils.getPreparedStatement(
                    dataSource, "SELECT " + COLUMN_CLOB_VALUE + " FROM "
                            + querySchema + TABLE_CLOB_VALUES + " WHERE "
                            + COLUMN_CLOB_CHECKSUM + " = ?");
            psSelect.setString(1, summaryChecksum);
            rsSelect = psSelect.executeQuery();

            if (!rsSelect.next()) return null;

            // get the configuration
            String summaryContent = rsSelect.getString(COLUMN_CLOB_VALUE);
            String[] attributes = summaryContent.split(",");
            for (int i = 0; i < attributes.length; i++) {
                attributes[i] = attributes[i].trim();
            }
            return attributes;
        } catch (SQLException ex) {
            throw new WdkUserException(ex);
        } finally {
            try {
                SqlUtils.closeResultSet(rsSelect);
            } catch (SQLException ex) {
                throw new WdkUserException(ex);
            }
        }
    }

    public String makeSortingChecksum(Map<String, Boolean> columns)
            throws WdkModelException, WdkUserException,
            NoSuchAlgorithmException {
        // create checksum for sorting columns
        StringBuffer sb = new StringBuffer();
        int i = 0;
        for (String colName : columns.keySet()) {
            i++;
            // only use a limit number of attribtues in sorting
            if (i >= Utilities.SORTING_LEVEL) break;

            boolean ascend = columns.get(colName);
            if (sb.length() > 0) sb.append(", ");
            sb.append(colName);
            sb.append(ascend ? " ASC" : " DESC");
        }
        String columnsContent = sb.toString();
        String checksum = Utilities.encrypt(columnsContent);

        // check if the sorting exists
        DataSource dataSource = platform.getDataSource();
        PreparedStatement psInsert = null;
        try {
            if (null != getSortingAttributes(checksum)) return checksum;

            // sorting not exists, add one
            psInsert = SqlUtils.getPreparedStatement(dataSource, "INSERT INTO"
                    + " " + querySchema + TABLE_CLOB_VALUES + " ("
                    + COLUMN_CLOB_CHECKSUM + ", " + COLUMN_CLOB_VALUE
                    + ") VALUES (?, ?)");
            psInsert.setString(1, checksum);
            psInsert.setString(2, columnsContent);
            psInsert.execute();

            return checksum;
        } catch (SQLException ex) {
            throw new WdkUserException(ex);
        } finally {
            try {
                SqlUtils.closeStatement(psInsert);
            } catch (SQLException ex) {
                throw new WdkUserException(ex);
            }
        }
    }

    public Map<String, Boolean> getSortingAttributes(String sortingChecksum)
            throws WdkUserException {
        DataSource dataSource = platform.getDataSource();
        ResultSet rsSelect = null;
        try {
            PreparedStatement psSelect = SqlUtils.getPreparedStatement(
                    dataSource, "SELECT " + COLUMN_CLOB_VALUE + " FROM "
                            + querySchema + TABLE_CLOB_VALUES + " WHERE "
                            + COLUMN_CLOB_CHECKSUM + " = ?");
            psSelect.setString(1, sortingChecksum);
            rsSelect = psSelect.executeQuery();

            if (!rsSelect.next()) return null;

            // get the sorting attributes
            String sortingContent = rsSelect.getString(COLUMN_CLOB_VALUE);
            String[] sortingPairs = sortingContent.split(",");
            Map<String, Boolean> attributes = new LinkedHashMap<String, Boolean>();
            for (String sortingPair : sortingPairs) {
                String[] pair = sortingPair.trim().split("\\s+");

                // validate the format of the pair
                if (pair.length != 2
                        || (!"ASC".equalsIgnoreCase(pair[1]) && !"DESC".equalsIgnoreCase(pair[1])))
                    throw new WdkUserException(
                            "Invalid sorting attribute format: '" + sortingPair
                                    + "'");
                String attribute = pair[0];
                boolean ascend = pair[1].equalsIgnoreCase("asc");
                attributes.put(attribute, ascend);
            }
            return attributes;
        } catch (SQLException ex) {
            throw new WdkUserException(ex);
        } finally {
            try {
                SqlUtils.closeResultSet(rsSelect);
            } catch (SQLException ex) {
                throw new WdkUserException(ex);
            }
        }
    }

    public String makeClobChecksum(String paramValue) throws WdkModelException,
            NoSuchAlgorithmException {
        // make the checksum
        String checksum = Utilities.encrypt(paramValue);

        DataSource dataSource = platform.getDataSource();
        PreparedStatement psInsert = null;
        try {
            // get the clob with the new checksum
            if (null != getClobValue(checksum)) return checksum;

            // clob value does not exist, add one
            psInsert = SqlUtils.getPreparedStatement(dataSource, "INSERT INTO"
                    + " " + querySchema + TABLE_CLOB_VALUES + " ("
                    + COLUMN_CLOB_CHECKSUM + ", " + COLUMN_CLOB_VALUE
                    + ") VALUES (?, ?)");
            psInsert.setString(1, checksum);
            psInsert.setString(2, paramValue);
            psInsert.execute();

            return checksum;
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            try {
                SqlUtils.closeStatement(psInsert);
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }
    }

    public String getClobValue(String paramChecksum) throws WdkModelException {
        DataSource dataSource = platform.getDataSource();
        ResultSet rs = null;
        try {
            PreparedStatement ps = SqlUtils.getPreparedStatement(dataSource,
                    "SELECT " + COLUMN_CLOB_VALUE + " FROM " + querySchema
                            + TABLE_CLOB_VALUES + " WHERE "
                            + COLUMN_CLOB_CHECKSUM + " = ?");
            ps.setString(1, paramChecksum);
            rs = ps.executeQuery();

            if (!rs.next()) return null;

            String clobValue = platform.getClobData(rs, COLUMN_CLOB_VALUE);
            return clobValue;
        } catch (SQLException ex) {
            throw new WdkModelException(ex);
        } finally {
            try {
                SqlUtils.closeResultSet(rs);
            } catch (SQLException ex) {
                throw new WdkModelException(ex);
            }
        }

    }
}
