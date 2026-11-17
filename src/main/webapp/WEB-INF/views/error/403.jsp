<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Access denied"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container narrow">
    <div class="error-page card">
        <div class="error-code">403</div>
        <h1>Access denied</h1>
        <c:set var="errMsg" value="${requestScope['jakarta.servlet.error.message']}"/>
        <p class="muted"><c:out value="${empty errMsg ? 'You do not have permission to view this page.' : errMsg}"/></p>
        <div class="hero-actions center">
            <a class="btn btn-primary" href="${ctx}/">Go to home</a>
            <c:if test="${empty sessionScope.currentUser}"><a class="btn btn-outline" href="${ctx}/login">Log in</a></c:if>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
