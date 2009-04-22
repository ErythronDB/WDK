<%@ taglib prefix="site" tagdir="/WEB-INF/tags/site" %>
<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>
<%@ taglib prefix="nested" uri="http://jakarta.apache.org/struts/tags-nested" %>
<%@ taglib prefix="fn" uri="http://java.sun.com/jsp/jstl/functions" %>


<dir>

  <!-- is node if current position of wdkAnswer is boolean -->
  <nested:define id="isNode" property="isBoolean"/> 
  <c:choose>
    <c:when test="${isNode}">
    <div>
       <nested:write property="booleanOperation"/><br>
       <nested:nest property="firstChildAnswer">
          <jsp:include page="/WEB-INF/includes/bqShowNode.jsp"/>
       </nested:nest>
    </div>
    <div>
       <nested:nest property="secondChildAnswer">
          <jsp:include page="/WEB-INF/includes/bqShowNode.jsp"/>
       </nested:nest>
    </div>
    </c:when>	
    <c:otherwise>
         <nested:define id="currentAnswer" property="this/"/>
         <%-- cannot displat the params, since we lost the history info --%>
         <%-- wdk:showParams wdkAnswer="${currentAnswer}" / --%>
    </c:otherwise>
  </c:choose>

</dir>
