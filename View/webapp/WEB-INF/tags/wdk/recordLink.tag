<?xml version="1.0" encoding="UTF-8"?>
<jsp:root version="2.0"
    xmlns:jsp="http://java.sun.com/JSP/Page"
    xmlns:c="http://java.sun.com/jsp/jstl/core">

  <jsp:directive.attribute
    name="primaryKeyAttributeValue"
    type="org.gusdb.wdk.model.record.attribute.PrimaryKeyAttributeValue"
    required="true"
    description="The primary key AttributeValue instance"
  />

  <jsp:directive.attribute
    name="recordName"
    required="false"
    description="The full name of the record class"
  />

  <c:set var="recordLinkKeys" value="" />
  <c:forEach items="${primaryKeyAttributeValue.values}" var="pkValue">
    <c:set var="recordLinkKeys" value="${recordLinkKeys}&amp;${pkValue.key}=${pkValue.value}" />
  </c:forEach>

  <c:url var="recordLink" value="/showRecord.do?name=${recordName}${recordLinkKeys}" />

  <a href="${recordLink}">${primaryKeyAttributeValue.value}</a>

</jsp:root>
