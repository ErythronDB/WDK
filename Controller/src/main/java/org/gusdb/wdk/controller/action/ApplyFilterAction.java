package org.gusdb.wdk.controller.action;

import java.net.URLEncoder;
import java.util.Enumeration;
import java.util.Iterator;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.controller.actionutil.ActionUtility;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.filter.Filter;
import org.gusdb.wdk.model.jspwrap.AnswerValueBean;
import org.gusdb.wdk.model.jspwrap.QuestionBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.json.JSONArray;
import org.json.JSONObject;

public class ApplyFilterAction extends Action {

  public static final String PARAM_FILTER = "filter";
  public static final String PARAM_STEP = "step";
  
  private static final Logger LOG = Logger.getLogger(ApplyFilterAction.class);

  @Override
  public ActionForward execute(ActionMapping mapping, ActionForm form, HttpServletRequest request,
      HttpServletResponse response) throws Exception {
    LOG.debug("Entering ApplyFilterAction...");
    
    String filterName = request.getParameter(PARAM_FILTER);
    if (filterName == null)
      throw new WdkUserException("Required filter parameter is missing.");
    String stepId = request.getParameter(PARAM_STEP);
    if (stepId == null)
      throw new WdkUserException("Required step parameter is missing.");
    JSONObject options = prepareOptions(request);
    
    UserBean user = ActionUtility.getUser(servlet, request);
    StepBean step = user.getStep(Integer.valueOf(stepId));
    AnswerValueBean answer = step.getAnswerValue();
    QuestionBean question = answer.getQuestion();
    Filter filter = question.getFilter(filterName);
    if (filter == null) 
      throw new WdkUserException("Filter \"" + filterName 
           + "\" cannot be found in question: " + question.getFullName());
    step.addFilterOption(filter.getKey(), options);
    
    ActionForward showStrategy = mapping.findForward(CConstants.SHOW_APPLICATION_MAPKEY);
    StringBuffer url = new StringBuffer(showStrategy.getPath());
    String state = request.getParameter(CConstants.WDK_STATE_KEY);
    url.append("?state=" + URLEncoder.encode(state, "UTF-8"));

    ActionForward forward = new ActionForward(url.toString());
    forward.setRedirect(true);
    LOG.debug("Leaving ApplyFilterAction.");
    return forward;
  }
  
  private JSONObject prepareOptions(HttpServletRequest request) {
    JSONObject jsOptions = new JSONObject();
    Enumeration<String> names = request.getParameterNames();
    while (names.hasMoreElements()) {
      String name = names.nextElement();
      String[] values = request.getParameterValues(name);
      if (values.length > 1) {
        JSONArray jsValues = new JSONArray();
        for (String value : values) {
          jsValues.put(value);
        }
        jsOptions.put(name, jsValues);
      } else {
        jsOptions.put(name, request.getParameter(name));
      }
    }
    return jsOptions;
  }
}
