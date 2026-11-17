<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="My account"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container">
    <h1 class="page-title">My account</h1>
    <div class="account-grid">
        <section class="card">
            <div class="profile-head">
                <div class="avatar"><c:out value="${fn:toUpperCase(fn:substring(user.fullName, 0, 1))}"/></div>
                <div>
                    <strong><c:out value="${user.fullName}"/></strong>
                    <span class="muted"><c:out value="${user.email}"/></span>
                    <span class="muted small">Member since <fmt:formatDate value="${user.createdDate}" pattern="MMMM yyyy"/></span>
                </div>
            </div>
            <div class="stat-row">
                <a class="stat" href="${ctx}/orders"><strong>${orderCount}</strong><span>Orders</span></a>
                <a class="stat" href="${ctx}/cart"><strong>${cartCount}</strong><span>In cart</span></a>
            </div>
        </section>

        <section class="card">
            <h3>Profile details</h3>
            <form action="${ctx}/account/profile" method="post" class="stack">
                <se:csrf/>
                <label class="field"><span>Full name</span>
                    <input type="text" name="fullName" required maxlength="120" value="<c:out value='${user.fullName}'/>"></label>
                <label class="field"><span>Email</span>
                    <input type="email" value="<c:out value='${user.email}'/>" disabled></label>
                <label class="field"><span>Mobile number</span>
                    <input type="tel" name="phone" maxlength="15" value="<c:out value='${user.phone}'/>"></label>
                <button type="submit" class="btn btn-primary">Save changes</button>
            </form>
        </section>

        <section class="card">
            <h3>Change password</h3>
            <form action="${ctx}/account/password" method="post" class="stack">
                <se:csrf/>
                <label class="field"><span>Current password</span>
                    <input type="password" name="currentPassword" required autocomplete="current-password"></label>
                <label class="field"><span>New password</span>
                    <input type="password" name="newPassword" required minlength="8" autocomplete="new-password"></label>
                <label class="field"><span>Confirm new password</span>
                    <input type="password" name="confirmPassword" required minlength="8" autocomplete="new-password"></label>
                <button type="submit" class="btn btn-outline">Update password</button>
            </form>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
