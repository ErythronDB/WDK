package org.gusdb.wdk.view.taglibs.query;

import java.io.IOException;

import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspTagException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

import org.gusdb.wdk.model.Query;
import org.gusdb.wdk.model.Summary;
import org.gusdb.wdk.model.SummarySet;
import org.gusdb.wdk.model.WdkModel;
import org.gusdb.wdk.model.WdkUserException;

/**
 * Custom tag which displays a Query to the user
 */
public class DisplayQuery extends SimpleTagSupport {
    
    private static final String DEFAULT_OPTION = "Choose...";
    private Summary question;
    
    public void setQuestion(Summary question) {
        this.question = question;
    }

// TODO Should it pick up other names through NullQuery???
    
    
    public void doTag() throws IOException, JspException {
    	JspWriter out = getJspContext().getOut();

        
        QueryHolder parent = (QueryHolder) findAncestorWithClass(this, QueryHolder.class);
        if (parent == null) {
            throw new JspTagException("The DisplayQuery tag is not enclosed by a QueryHolder tag");
        }
        String questionSetName = parent.getQuestionSetName();
            


        
    	if ( question == null) {
            WdkModel wm = (WdkModel) getJspContext().getAttribute("wdk.wdkModel", PageContext.APPLICATION_SCOPE);
            SummarySet rls = null;
            try {
                rls = wm.getSummarySet(questionSetName);
            } catch (WdkUserException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }
            Summary[] rla = rls.getSummaries();
            
            
//    		Query[] sq = sqs.getQueries();
    		out.println("<b>Queries:</b> <select name=\"questionName\">");
    		out.println("<option value=\""+DEFAULT_OPTION+"\">"+DEFAULT_OPTION);
    		for ( int i=0 ; i < rla.length ;i++) {
                Query sq = rla[i].getQuery();
    			String val = sq.getDisplayName();
    			out.println("<option value=\""+rls.getName()+"."+rla[i].getName()+"\">"+val);
    		}
    		out.println("</select>");
    		out.println("<input type=\"hidden\" name=\"defaultChoice\" value=\""+DEFAULT_OPTION+"\">");
            out.println("<input type=\"hidden\" name=\"initialExpansion\" value=\"true\">");
    		return;
    	}
        
    	out.println("<h4>"+question.getQuery().getDisplayName()+"</h4>");
    	out.println("<input type=\"hidden\" name =\"questionName\" value=\""+questionSetName+"."+question.getName()+"\">");
    	out.println("<input type=\"hidden\" name=\"defaultChoice\" value=\""+DEFAULT_OPTION+"\">");

        if (getJspBody() != null) {
            //out.println("<br>I'm trying to set wdk.queryName to fred<br>");
            getJspContext().setAttribute("wdk.queryName", questionSetName +"."+question.getName(), PageContext.PAGE_SCOPE);
            getJspBody().invoke(null);
            getJspContext().removeAttribute("wdk.queryName", PageContext.PAGE_SCOPE);
    	}
  
    }

}
