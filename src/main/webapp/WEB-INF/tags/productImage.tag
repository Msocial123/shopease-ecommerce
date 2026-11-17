<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Product image or generated SVG placeholder" %>
<%@ attribute name="id" required="true" type="java.lang.Object" %>
<%@ attribute name="url" required="false" type="java.lang.String" %>
<%@ attribute name="alt" required="false" type="java.lang.String" %>
<%@ attribute name="cssClass" required="false" type="java.lang.String" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="base" value="${pageContext.request.contextPath}"/>
<c:choose>
    <c:when test="${empty url}"><c:set var="src" value="${base}/images/product/${id}.svg"/></c:when>
    <c:when test="${fn:startsWith(url, '/')}"><c:set var="src" value="${base}${url}"/></c:when>
    <c:otherwise><c:set var="src" value="${url}"/></c:otherwise>
</c:choose>
<img src="<c:out value='${src}'/>" alt="<c:out value='${alt}'/>" class="${cssClass}" loading="lazy"
     onerror="this.onerror=null;this.src='${base}/images/product/${id}.svg'">
