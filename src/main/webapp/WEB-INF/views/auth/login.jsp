<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Log in"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container auth-wrap">
    <div class="card auth-card">
        <h1>Welcome back</h1>
        <p class="muted">Log in to track orders, save your cart and check out faster.</p>
        <c:if test="${not empty error}"><div class="notice notice-error"><c:out value="${error}"/></div></c:if>
        <form action="${ctx}/login" method="post" class="stack">
            <se:csrf/>
            <input type="hidden" name="redirect" value="<c:out value='${redirect}'/>">
            <label class="field">
                <span>Email</span>
                <input type="email" name="email" required autofocus autocomplete="username" value="<c:out value='${email}'/>">
            </label>
            <label class="field">
                <span>Password</span>
                <input type="password" name="password" required autocomplete="current-password">
            </label>
            <button type="submit" class="btn btn-primary btn-block btn-lg">Log in</button>
        </form>
        <p class="center">New here? <a href="${ctx}/register">Create an account</a></p>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
