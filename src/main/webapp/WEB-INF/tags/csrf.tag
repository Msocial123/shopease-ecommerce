<%@ tag body-content="empty" pageEncoding="UTF-8" description="Hidden CSRF token field for POST forms" %>
<input type="hidden" name="_csrf" value="${requestScope.csrfToken}">
