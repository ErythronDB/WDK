/**
 * 
 */
package org.gusdb.wdk.model;

import java.io.IOException;

import org.junit.Assert;
import org.xml.sax.SAXException;

/**
 * @author Jerric
 * 
 */
public class ModelConfigParserTest {

    private static final String SAMPLE_PROJECT_ID = "SampleDB";

    private String projectId;
    private String gusHome;

    /**
     * get and validate the input
     * 
     * @throws WdkModelException
     */
    @org.junit.Before
    public void getInput() throws WdkModelException {
        // get input from the system environment
        projectId = System.getProperty(Utilities.ARGUMENT_PROJECT_ID);
        gusHome = System.getProperty(Utilities.SYSTEM_PROPERTY_GUS_HOME);

        // GUS_HOME is required
        if (gusHome == null || gusHome.length() == 0)
            throw new WdkModelException("Required "
                    + Utilities.SYSTEM_PROPERTY_GUS_HOME
                    + " property is missing.");

        // project id is optional
        if (projectId == null || projectId.length() == 0)
            projectId = SAMPLE_PROJECT_ID;
    }

    /**
     * test parsing a valid config file
     * 
     * @throws IOException
     * @throws SAXException
     * @throws WdkModelException
     */
    @org.junit.Test
    public void testParseConfig() throws SAXException, IOException,
            WdkModelException {
        ModelConfigParser parser = new ModelConfigParser(gusHome);
        ModelConfig config = parser.parseConfig(projectId);
        Assert.assertNotNull(config);
    }

    /**
     * parse an invalid configuration file
     * 
     * @throws IOException
     * @throws SAXException
     * @throws WdkModelException
     */
    @org.junit.Test(expected = WdkModelException.class)
    public void testParseInvalidConfig() throws SAXException, IOException,
            WdkModelException {
        String projectId = "SampleDB_bad_config_syntax";
        ModelConfigParser parser = new ModelConfigParser(gusHome);
        parser.parseConfig(projectId);
    }
}
