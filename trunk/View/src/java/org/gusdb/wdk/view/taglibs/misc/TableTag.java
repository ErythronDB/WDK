package org.gusdb.gus.wdk.view.taglibs.misc;

import java.io.IOException;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.JspWriter;
import javax.servlet.jsp.tagext.*;


public class TableTag extends TagSupport {
  
  public int rowPosition = 0;

  public TableTag() {
    super();
  }

  public void incrementRowPos() {
    rowPosition++;
  }
    
  public int getRowPosition() {
    return rowPosition;
  }
  
  public int doStartTag() throws JspException {
   	JspWriter out = pageContext.getOut();
    try {
      out.println("<table>");
    }
    catch (IOException ie){
    }
   	return EVAL_BODY_INCLUDE;
  }
   
  public int doEndTag() throws JspException {
   	JspWriter out = pageContext.getOut();
    try {
      out.println("</table>");
    }
    catch (IOException ie) {
    }
    rowPosition = 0;
    return EVAL_PAGE;
  }
}