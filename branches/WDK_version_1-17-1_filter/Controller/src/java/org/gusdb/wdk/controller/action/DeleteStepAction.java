package org.gusdb.wdk.controller.action;

import java.net.URLEncoder;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.apache.struts.action.ActionServlet;
import org.apache.struts.upload.FormFile;
import org.gusdb.wdk.controller.ApplicationInitListener;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.WdkUserException;
import org.gusdb.wdk.model.jspwrap.RecordPageBean;
import org.gusdb.wdk.model.jspwrap.UserAnswerBean;
import org.gusdb.wdk.model.jspwrap.UserStrategyBean;
import org.gusdb.wdk.model.jspwrap.StepBean;
import org.gusdb.wdk.model.jspwrap.UserBean;
import org.gusdb.wdk.model.jspwrap.WdkModelBean;

/**
 *  This Action handles moving a step in a search strategy to a different
 *  position.  It moves the step, updates the relevant filter userAnswers,
 *  and forwards to ShowSummaryAction
 **/

public class DeleteStepAction extends Action {
    private static final Logger logger = Logger.getLogger(DeleteStepAction.class);
    
    public ActionForward execute( ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        System.out.println("Entering DeleteStepAction...");


	// Make sure a strategy is specified
	String strProtoId = request.getParameter("strategy");

	System.out.println("Filter strategy: " + strProtoId);
	if (strProtoId == null || strProtoId.length() == 0) {
	    throw new WdkModelException("No strategy was specified for deleting a step!");
	}

	// load model, user
	WdkModelBean wdkModel = ( WdkModelBean ) servlet.getServletContext().getAttribute(CConstants.WDK_MODEL_KEY );
        UserBean wdkUser = ( UserBean ) request.getSession().getAttribute(
                CConstants.WDK_USER_KEY );
        if ( wdkUser == null ) {
            wdkUser = wdkModel.getUserFactory().getGuestUser();
            request.getSession().setAttribute( CConstants.WDK_USER_KEY, wdkUser );
        }

	UserAnswerBean userAnswer, filterHist;
        RecordPageBean wdkRecordPage;
	UserStrategyBean strategy;
	StepBean step;
	String boolExp;

	// Are we revising or deleting a step?
	String deleteStep = request.getParameter("delete");

	if (deleteStep == null || deleteStep.length() == 0) {
	    throw new WdkModelException("No step was specified to delete!");
	}

	strategy = wdkUser.getUserStrategy(Integer.parseInt(strProtoId));
	step = strategy.getStep(Integer.valueOf(deleteStep));

	/* Charles:  Commented out on 7/1/08; need to rewrite for new strategy object
	// are we deleting the first step?
	if (step.getIsFirstStep()) {
	    // if there are two steps, we're just moving to the second step as a one-step strategy
	    if (strategy.getLength() == 2) {
		// will need to change when we have unique ids for strategys
		strProtoId = step.getNextStep().getSubQueryUserAnswer().getUserAnswerId() + "";
	    }
	    // if there are more than two steps, we need to update the filter userAnswer of the third step
	    // so that the boolean expression points to the subquery userAnswer of the second step
	    else if (strategy.getLength() > 2) {
		step = step.getNextStep().getNextStep();
		filterHist = step.getFilterUserAnswer();
		boolExp = filterHist.getBooleanExpression();
		boolExp = step.getPreviousStep().getSubQueryUserAnswer().getUserAnswerId() + boolExp.substring(boolExp.indexOf(" "), boolExp.length());
		wdkUser.updateUserAnswer(filterHist, boolExp);
	    }
	    // not sure what to do here, but something has to happen...
	    else {
		// eventually we'll support deleting strategys...?
		// for now, throw error
		//throw new WdkUserException("Can't delete the only step in a one-step search strategy!");
		ActionForward forward = new ActionForward("");
		forward.setRedirect( true );
		return forward;
	    }
	}
	else {
	    //if this is not the last step, then filter userAnswer of the next step needs
	    // to point to filter userAnswer of the previous step
	    if (Integer.valueOf(deleteStep) < strategy.getLength() - 1) {
		filterHist = step.getNextStep().getFilterUserAnswer();
		boolExp = filterHist.getBooleanExpression();
		boolExp = step.getPreviousStep().getFilterUserAnswer().getUserAnswerId() + " " + boolExp.substring(boolExp.indexOf(" "), boolExp.length());
		wdkUser.updateUserAnswer(filterHist, boolExp);
	    }
	    //if this is the last step, we're just moving to the strategy that ends w/ the previous step
	    else {
		strProtoId = step.getPreviousStep().getFilterUserAnswer().getUserAnswerId() + "";
	    }
	}
	*/

	// 5. forward to showsummary
	ActionForward showSummary = mapping.findForward( CConstants.SHOW_SUMMARY_MAPKEY );
	StringBuffer url = new StringBuffer( showSummary.getPath() );
	url.append("?strategy=" + URLEncoder.encode(strProtoId));
	String viewStep = request.getParameter("step");
	if (viewStep != null && viewStep.length() != 0) {
	    if (Integer.valueOf(viewStep) > Integer.valueOf(deleteStep)) {
		viewStep = (Integer.valueOf(viewStep) - 1) + "";
		url.append("&step=" + URLEncoder.encode(viewStep));
	    }
	    else if (Integer.valueOf(viewStep) < Integer.valueOf(deleteStep)) {
		url.append("&step=" + URLEncoder.encode(viewStep));
	    }
	}
	String subQuery = request.getParameter("subquery");
	if (subQuery != null && subQuery.length() != 0) {
	    url.append("&subquery=" + URLEncoder.encode(subQuery));
	}
	System.out.println(url.toString());
	ActionForward forward = new ActionForward( url.toString() );
	forward.setRedirect( true );
	return forward;
    }
}
