package org.gusdb.wdk.controller.action;

import java.io.File;
import java.util.Map;

import javax.servlet.ServletContext;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.apache.struts.action.Action;
import org.apache.struts.action.ActionForm;
import org.apache.struts.action.ActionForward;
import org.apache.struts.action.ActionMapping;
import org.gusdb.wdk.controller.ApplicationInitListener;
import org.gusdb.wdk.controller.CConstants;
import org.gusdb.wdk.model.WdkModelException;
import org.gusdb.wdk.model.jspwrap.AnswerBean;
import org.gusdb.wdk.model.jspwrap.HistoryBean;
import org.gusdb.wdk.model.jspwrap.RecordClassBean;
import org.gusdb.wdk.model.jspwrap.UserBean;

/**
 * This Action is process the download of Answers on queryHistory.jsp page.
 * 
 */

public class DownloadHistoryAnswerAction extends Action {
    
    private static Logger logger = Logger.getLogger( DownloadHistoryAnswerAction.class );
    
    public ActionForward execute( ActionMapping mapping, ActionForm form,
            HttpServletRequest request, HttpServletResponse response )
            throws Exception {
        AnswerBean wdkAnswer = getAnswerBean( request );
        
        // get a list of supported reporters
        RecordClassBean recordClass = wdkAnswer.getQuestion().getRecordClass();
        String fullName = recordClass.getFullName();
        Map< String, String > reporters = recordClass.getReporters();
        
        // get the current selected reporter
        String reporter = request.getParameter( CConstants.WDK_REPORT_FORMAT_KEY );
        if ( reporter != null && reporter.trim().length() == 0 )
            reporter = null;
        
        request.setAttribute( CConstants.WDK_REPORT_FORMATS_KEY, reporters );
        if ( reporter != null ) {
            request.setAttribute( CConstants.WDK_REPORT_FORMAT_KEY, reporter );
        }
        request.setAttribute( CConstants.WDK_ANSWER_KEY, wdkAnswer );
        request.setAttribute( CConstants.WDK_QUESTION_PARAMS_KEY,
                wdkAnswer.getInternalParams() );
        
        // get forward
        ActionForward forward;
        
        if ( reporter == null ) {
            // get the default configuration page
            forward = mapping.findForward( CConstants.GET_DOWNLOAD_CONFIG_MAPKEY );
        } else {
            ServletContext svltCtx = getServlet().getServletContext();
            String customViewDir = ( String ) svltCtx.getAttribute( CConstants.WDK_CUSTOMVIEWDIR_KEY );
            String customViewFile1 = customViewDir + File.separator + fullName
                    + "." + reporter + "ReporterConfig.jsp";
            String customViewFile2 = customViewDir + File.separator + reporter
                    + "ReporterConfig.jsp";
            String customViewFile3 = "/" + reporter
                    + "ReporterConfig.jsp";
            
            if ( ApplicationInitListener.resourceExists( customViewFile1,
                    svltCtx ) ) {
                forward = new ActionForward( customViewFile1 );
            } else if ( ApplicationInitListener.resourceExists(
                    customViewFile2, svltCtx ) ) {
                forward = new ActionForward( customViewFile2 );
            } else if ( ApplicationInitListener.resourceExists(
                    customViewFile3, svltCtx ) ) {
                forward = new ActionForward( customViewFile3 );
            } else {
                throw new WdkModelException( "No configuration form can be "
                        + "found for the selected format: " + reporter );
            }
        }
        logger.info( "The download config: " + forward.getPath() );
        
        return forward;
    }
    
    protected AnswerBean getAnswerBean( HttpServletRequest request )
            throws Exception {
        String histIdstr = request.getParameter( CConstants.WDK_HISTORY_ID_KEY );
        if ( histIdstr == null ) {
            histIdstr = ( String ) request.getAttribute( CConstants.WDK_HISTORY_ID_KEY );
        }
        if ( histIdstr != null ) {
            int histId = Integer.parseInt( histIdstr );
            request.setAttribute( CConstants.WDK_HISTORY_ID_KEY, histId );
            
            UserBean wdkUser = ( UserBean ) request.getSession().getAttribute(
                    CConstants.WDK_USER_KEY );
            
            HistoryBean history = wdkUser.getHistory( histId );
            request.setAttribute(CConstants.WDK_HISTORY_KEY, history);
            return history.getAnswer();
        } else {
            throw new Exception(
                    "no history id is given for which to download the result" );
        }
    }
}
