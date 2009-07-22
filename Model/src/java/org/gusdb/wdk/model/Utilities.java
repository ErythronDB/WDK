package org.gusdb.wdk.model;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.Clob;
import java.sql.SQLException;
import java.util.Map;
import java.util.regex.Pattern;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class Utilities {

    public static final int TRUNCATE_DEFAULT = 100;

    /**
     * param values with is prefix means the result of that value is a checksum
     * for the real value. if the param is a datasetParam, then the checksum is
     * an index to a dataset obejct; if the param is of other types, then the
     * value of the param can be found in the clob values table by the checksum
     */
    public static final String PARAM_COMPRESSE_PREFIX = "[C]";
    public static final String DATASET_PARAM_KEY_PREFIX = "[K]";

    /**
     * The maximum size for parameter values that will be displayed in thr URL
     * as plain values
     */
    public static final int MAX_PARAM_VALUE_SIZE = 100;

    /**
     * The maximum number of attributes used in sorting an answer
     */
    public static final int SORTING_LEVEL = 3;

    /**
     * command-line argument: -model
     */
    public static final String ARGUMENT_PROJECT_ID = "model";

    /**
     * system property: gusHome
     */
    public static final String SYSTEM_PROPERTY_GUS_HOME = "GUS_HOME";

    public static final int MAXIMUM_RECORD_INSTANCES = 10000;

    public static final String COLUMN_PROJECT_ID = "project_id";

    public static final String PARAM_PROJECT_ID = COLUMN_PROJECT_ID;

    public static final String ALIAS_OLD_KEY_COLUMN_PREFIX = "old_";

    public static final String INTERNAL_PARAM_SET = "InternalParams";

    public static final String INTERNAL_QUERY_SET = "InternalQueries";

    public static final String INTERNAL_QUESTION_SET = "InternalQuestions";

    public static final int DEFAULT_PAGE_SIZE = 20;

    public static final int DEFAULT_SUMMARY_ATTRIBUTE_SIZE = 6;

    public static String encrypt(String data) throws WdkModelException,
            NoSuchAlgorithmException {
        return encrypt(data, false);
    }

    public static String encrypt(String data, boolean shortDigest)
            throws WdkModelException, NoSuchAlgorithmException {
        // cannot encrypt null value
        if (data == null || data.length() == 0)
            throw new WdkModelException("Cannot encrypt an empty/null string");

        MessageDigest digest = MessageDigest.getInstance("MD5");
        byte[] byteBuffer = digest.digest(data.toString().getBytes());
        if (shortDigest) {
            // just take the first 8 bytes from MD5 hash
            int size = Math.min(byteBuffer.length, 8);
            byte[] newBuffer = new byte[size];
            System.arraycopy(byteBuffer, 0, newBuffer, 0, newBuffer.length);
            byteBuffer = newBuffer;
        }
        // convert each byte into hex format
        StringBuffer buffer = new StringBuffer();
        for (int i = 0; i < byteBuffer.length; i++) {
            int code = (byteBuffer[i] & 0xFF);
            if (code < 0x10) buffer.append('0');
            buffer.append(Integer.toHexString(code));
        }
        return buffer.toString();
    }

    public static String replaceMacros(String text, Map<String, Object> tokens)
            throws WdkModelException, NoSuchAlgorithmException, SQLException,
            JSONException {
        for (String token : tokens.keySet()) {
            Object object = tokens.get(token);
            String value = (object == null) ? "" : object.toString();
            String macro = Pattern.quote("$$" + token + "$$");
            text = text.replaceAll(macro, value);
        }
        return text;
    }

    public static String[] toArray(String data) {
        if (data == null || data.length() == 0) {
            String[] values = new String[0];
            return values;
        }
        data = data.replace(',', ' ');
        data = data.replace(';', ' ');
        data = data.replace('\t', ' ');
        data = data.replace('\n', ' ');
        data = data.replace('\r', ' ');
        return data.trim().split("\\s+");
    }

    public static String fromArray(String[] data) {
        return fromArray(data, ",");
    }

    public static String fromArray(String[] data, String delimiter) {
        if (data == null) return null;
        StringBuffer sb = new StringBuffer();
        for (String value : data) {
            if (sb.length() > 0) sb.append(delimiter);
            sb.append(value);
        }
        return sb.toString();
    }

    public static String parseValue(Object objValue) throws SQLException {
        String value;
        if (objValue == null) value = null;
        else if (objValue instanceof Clob) {
            Clob clob = (Clob) objValue;
            value = clob.getSubString(1, (int) clob.length());
        } else value = objValue.toString();
        return value;
    }
    
    public static String[][] convertContent(String content) throws JSONException {
        JSONArray jsResult = new JSONArray(content);
        JSONArray jsRow = (JSONArray) jsResult.get(0);
        String[][] result = new String[jsResult.length()][jsRow.length()];
        for (int row = 0; row < result.length; row++) {
            jsRow = (JSONArray) jsResult.get(row);
            for (int col = 0; col < result[row].length; col++) {
                Object cell = jsRow.get(col);
                result[row][col] = (cell == null || cell == JSONObject.NULL) ? 
                    null : cell.toString();
            }
        }
        return result;
    }

}
