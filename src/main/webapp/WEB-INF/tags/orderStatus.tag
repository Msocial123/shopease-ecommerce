<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Coloured status pill for orders / payments" %>
<%@ attribute name="status" required="true" type="java.lang.Object" %>
<%@ attribute name="label" required="false" type="java.lang.String" %>
<span class="status status-${status}">${empty label ? status : label}</span>
