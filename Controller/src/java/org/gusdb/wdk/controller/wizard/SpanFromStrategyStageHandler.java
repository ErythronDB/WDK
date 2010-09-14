package org.gusdb.wdk.controller.wizard;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.struts.action.ActionServlet;
import org.gusdb.wdk.controller.action.ActionUtility;
import org.gusdb.wdk.controller.action.WizardForm;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.StrategyBean;
import org.gusdb.wdk.model.jspwrap.UserBean;

public class SpanFromStrategyStageHandler implements StageHandler {

    private static final String PARAM_IMPORT_STRATEGY = "importStrategy";
    private static final String ATTR_IMPORT_STEP = "importStep";

    public void execute(ActionServlet servlet, HttpServletRequest request,
            HttpServletResponse response, WizardForm wizardForm)
            throws Exception {
        // load strategy
        String strStratId = request.getParameter(PARAM_IMPORT_STRATEGY);
        if (strStratId == null || strStratId.length() == 0)
            throw new WdkUserException("required "+PARAM_IMPORT_STRATEGY+" is missing.");
        
        int strategyId = Integer.valueOf(strStratId);
        UserBean user = ActionUtility.getUser(servlet, request);
        StrategyBean strategy = user.getStrategy(strategyId);
        StepBean step = strategy.getLatestStep();
        
        request.setAttribute(ATTR_IMPORT_STEP, step);
    }
}
