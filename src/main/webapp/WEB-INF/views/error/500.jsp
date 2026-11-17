<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="${empty title ? 'Something went wrong' : title}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container narrow">
    <div class="error-page card">
        <div class="error-code">Oops</div>
        <h1><c:out value="${pageTitle}"/></h1>
        <p class="muted"><c:out value="${empty message ? 'An unexpected error occurred. Please try again in a moment.' : message}"/></p>
        <div class="hero-actions center">
            <a class="btn btn-primary" href="${ctx}/">Go to home</a>
            <a class="btn btn-outline" href="javascript:history.back()">Go back</a>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
