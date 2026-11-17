<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Page not found"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container narrow">
    <div class="error-page card">
        <div class="error-code">404</div>
        <h1>We can't find that page</h1>
        <p class="muted"><c:out value="${empty message ? 'The page you are looking for may have been moved or no longer exists.' : message}"/></p>
        <div class="hero-actions center">
            <a class="btn btn-primary" href="${ctx}/">Go to home</a>
            <a class="btn btn-outline" href="${ctx}/products">Browse products</a>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
