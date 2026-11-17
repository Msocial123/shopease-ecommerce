<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Formats an amount with the store currency" %>
<%@ attribute name="value" required="true" type="java.lang.Object" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
${applicationScope.currency}<fmt:formatNumber value="${value}" minFractionDigits="2" maxFractionDigits="2" groupingUsed="true"/>
