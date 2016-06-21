package org.gusdb.wdk.controller;

import java.io.IOException;

import javax.servlet.ServletContext;

import org.apache.log4j.Logger;
import org.gusdb.fgputil.runtime.GusHome;
import org.gusdb.wdk.controller.wizard.Wizard;
import org.gusdb.wdk.model.MDCUtil;
import org.gusdb.wdk.model.Utilities;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;
import org.xml.sax.SAXException;

public class WdkInitializer {

  private static final Logger logger = Logger.getLogger(ApplicationInitListener.class);

  public static void initializeWdk(ServletContext servletContext) {

    try {
      setLogParams("init");
      logger.info("Initializing WDK web application");

      String gusHome = GusHome.webInit(servletContext);
      String projectId = servletContext.getInitParameter(Utilities.ARGUMENT_PROJECT_ID);
      String alwaysGoToSummary = servletContext.getInitParameter(CConstants.WDK_ALWAYSGOTOSUMMARY_PARAM);
      String loginUrl = servletContext.getInitParameter(CConstants.WDK_LOGIN_URL_PARAM);

      initMemberVars(servletContext, projectId, gusHome, alwaysGoToSummary, loginUrl);
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    finally {
      MDCUtil.clearValues();
    }
  }
  public static void terminateWdk(ServletContext servletContext) {
    try {
      setLogParams("term");
      logger.info("Terminating WDK web application");

      WdkModelBean wdkModel = getWdkModel(servletContext);
      if (wdkModel != null) {
        // insulate in case model never properly loaded
        logger.info("Releasing resources for WDK Model.");
        wdkModel.getModel().releaseResources();
      }
    }
    catch (Exception ex) {
      throw new RuntimeException(ex);
    }
    finally {
      MDCUtil.clearValues();
    }
  }

  private static void initMemberVars(ServletContext servletContext, String projectId,
      String gusHome, String alwaysGoToSummary, String loginUrl)
          throws WdkModelException, IOException, SAXException {

    logger.info("Initializing model...");
    WdkModel wdkModelRaw = WdkModel.construct(projectId, gusHome);
    WdkModelBean wdkModel = new WdkModelBean(wdkModelRaw);

    logger.info("Initialized model object.  Setting on servlet context.");
    servletContext.setAttribute(CConstants.WDK_MODEL_KEY, wdkModel);

    // Set assetsUrl attribute. It will be null if not defined in the model
    servletContext.setAttribute(CConstants.WDK_ASSETS_URL_KEY, wdkModel.getModel().getModelConfig().getAssetsUrl());

    // load wizard
    Wizard wizard = Wizard.loadWizard(gusHome, wdkModel);
    servletContext.setAttribute(CConstants.WDK_WIZARD_KEY, wizard);
    servletContext.setAttribute(CConstants.WDK_ALWAYSGOTOSUMMARY_KEY, alwaysGoToSummary);
    servletContext.setAttribute(CConstants.WDK_LOGIN_URL_KEY, loginUrl);
    servletContext.setAttribute(CConstants.GUS_HOME_KEY, gusHome);
  }

  public static WdkModelBean getWdkModel(ServletContext servletContext) {
    return (WdkModelBean)servletContext.getAttribute(CConstants.WDK_MODEL_KEY);
  }

  private static void setLogParams(String stage) {
    MDCUtil.setRequestId(stage);
    MDCUtil.setSessionId(stage);
    MDCUtil.setIpAddress("<no_ip_address>");
    MDCUtil.setRequestStartTime(System.currentTimeMillis());
  }
}
