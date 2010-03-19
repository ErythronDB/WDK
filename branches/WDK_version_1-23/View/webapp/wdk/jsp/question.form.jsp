<%@ taglib prefix="wdk" tagdir="/WEB-INF/tags/wdk" %>
<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="html" uri="http://jakarta.apache.org/struts/tags-html" %>

<c:set var="customForm" value="${requestScope.customForm}" />

<c:choose>
  <c:when test="${customForm != null}">
    <jsp:include page="${customForm}" />
  </c:when>
  <c:otherwise>
    <wdk:question/>
  </c:otherwise>
</c:choose>
