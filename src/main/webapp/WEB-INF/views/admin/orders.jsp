<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Manage orders"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<c:url var="baseUrl" value="/admin/orders">
    <c:if test="${not empty status}"><c:param name="status" value="${status.name}"/></c:if>
    <c:if test="${not empty q}"><c:param name="q" value="${q}"/></c:if>
</c:url>

<div class="container admin-layout">
    <%@ include file="/WEB-INF/views/common/admin-nav.jspf" %>
    <div class="admin-main">
        <h1 class="page-title">Orders <small class="muted">(${result.totalElements})</small></h1>
        <form class="card toolbar-form" method="get" action="${ctx}/admin/orders">
            <input type="search" name="q" placeholder="Order no., customer name or email" value="<c:out value='${q}'/>">
            <select name="status">
                <option value="">All statuses</option>
                <c:forEach var="s" items="${statuses}">
                    <option value="${s.name}" ${status == s ? 'selected' : ''}><c:out value="${s.label}"/></option>
                </c:forEach>
            </select>
            <button class="btn" type="submit">Filter</button>
        </form>

        <div class="card table-wrap">
            <table class="table">
                <thead><tr><th>Order</th><th>Date</th><th>Customer</th><th>Payment</th><th>Status</th><th class="num">Items</th><th class="num">Total</th></tr></thead>
                <tbody>
                <c:forEach var="o" items="${result.content}">
                    <tr>
                        <td><a href="${ctx}/admin/orders/${o.orderNumber}"><strong><c:out value="${o.orderNumber}"/></strong></a></td>
                        <td><fmt:formatDate value="${o.createdDate}" pattern="dd MMM yy, HH:mm"/></td>
                        <td><c:out value="${o.customerName}"/><div class="muted small"><c:out value="${o.customerEmail}"/></div></td>
                        <td><c:out value="${o.paymentMethod.label}"/></td>
                        <td><se:orderStatus status="${o.status}" label="${o.status.label}"/></td>
                        <td class="num">${o.itemCount}</td>
                        <td class="num"><se:money value="${o.total}"/></td>
                    </tr>
                </c:forEach>
                <c:if test="${empty result.content}"><tr><td colspan="7" class="center muted">No orders found.</td></tr></c:if>
                </tbody>
            </table>
        </div>
        <se:pagination pager="${result}" url="${baseUrl}"/>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
