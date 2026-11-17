<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Create account"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container auth-wrap">
    <div class="card auth-card">
        <h1>Create your account</h1>
        <p class="muted">It takes less than a minute.</p>
        <c:if test="${not empty error}"><div class="notice notice-error"><c:out value="${error}"/></div></c:if>
        <form action="${ctx}/register" method="post" class="stack">
            <se:csrf/>
            <label class="field">
                <span>Full name</span>
                <input type="text" name="fullName" required maxlength="120" autocomplete="name" value="<c:out value='${fullName}'/>">
            </label>
            <label class="field">
                <span>Email</span>
                <input type="email" name="email" required maxlength="160" autocomplete="email" value="<c:out value='${email}'/>">
            </label>
            <label class="field">
                <span>Mobile number <small class="muted">(optional)</small></span>
                <input type="tel" name="phone" maxlength="15" autocomplete="tel" value="<c:out value='${phone}'/>">
            </label>
            <div class="row-2">
                <label class="field">
                    <span>Password</span>
                    <input type="password" name="password" required minlength="8" autocomplete="new-password">
                </label>
                <label class="field">
                    <span>Confirm password</span>
                    <input type="password" name="confirmPassword" required minlength="8" autocomplete="new-password">
                </label>
            </div>
            <p class="muted small">At least 8 characters with letters and numbers.</p>
            <button type="submit" class="btn btn-primary btn-block btn-lg">Create account</button>
        </form>
        <p class="center">Already have an account? <a href="${ctx}/login">Log in</a></p>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
