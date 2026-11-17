<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Customers"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container admin-layout">
    <%@ include file="/WEB-INF/views/common/admin-nav.jspf" %>
    <div class="admin-main">
        <h1 class="page-title">Users <small class="muted">(${fn:length(users)})</small></h1>
        <div class="card table-wrap">
            <table class="table">
                <thead><tr><th>Name</th><th>Email</th><th>Phone</th><th>Role</th><th>Joined</th><th>Status</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="u" items="${users}">
                    <tr class="${u.active ? '' : 'row-muted'}">
                        <td><c:out value="${u.fullName}"/></td>
                        <td><c:out value="${u.email}"/></td>
                        <td><c:out value="${u.phone}"/></td>
                        <td><span class="tag">${u.role}</span></td>
                        <td><fmt:formatDate value="${u.createdDate}" pattern="dd MMM yyyy"/></td>
                        <td><span class="status ${u.active ? 'status-PAID' : 'status-CANCELLED'}">${u.active ? 'Active' : 'Disabled'}</span></td>
                        <td class="actions">
                            <c:if test="${u.id != sessionScope.currentUser.id}">
                                <form action="${ctx}/admin/users/${u.id}/toggle" method="post" class="inline-form">
                                    <se:csrf/>
                                    <button class="btn btn-sm btn-link ${u.active ? 'btn-danger' : ''}" type="submit">${u.active ? 'Disable' : 'Enable'}</button>
                                </form>
                            </c:if>
                        </td>
                    </tr>
                </c:forEach>
                </tbody>
            </table>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
