/*
 * Created on Jun 24, 2004
 *
 * TODO To change the template for this generated file go to
 * Window - Preferences - Java - Code Generation - Code and Comments
 */
package org.gusdb.wdk.model;

public interface ModelSetI {

    String getName();

    Object getElement(String elementName);

    void setResources(WdkModel model) throws WdkModelException;

    void resolveReferences(WdkModel model) throws WdkModelException;

}
