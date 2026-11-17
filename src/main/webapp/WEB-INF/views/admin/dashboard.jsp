<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Admin dashboard"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container admin-layout">
    <%@ include file="/WEB-INF/views/common/admin-nav.jspf" %>
    <div class="admin-main">
        <h1 class="page-title">Dashboard</h1>

        <div class="kpi-grid">
            <div class="kpi card"><span>Revenue</span><strong><se:money value="${revenue}"/></strong><small class="muted">Paid, shipped &amp; delivered</small></div>
            <div class="kpi card"><span>Orders</span><strong>${orderCount}</strong><small class="muted">${statusCounts['PAID'] + statusCounts['PLACED']} awaiting shipment</small></div>
            <div class="kpi card"><span>Customers</span><strong>${customerCount}</strong><small class="muted">${activeSessions} active sessions</small></div>
            <div class="kpi card"><span>Live products</span><strong>${productCount}</strong><small class="muted">${fn:length(lowStock)} low on stock</small></div>
        </div>

        <section class="card">
            <h3>Orders by status</h3>
            <div class="status-bars">
                <c:forEach var="e" items="${statusCounts}">
                    <a class="status-bar" href="${ctx}/admin/orders?status=${e.key}">
                        <se:orderStatus status="${e.key}"/>
                        <strong>${e.value}</strong>
                    </a>
                </c:forEach>
            </div>
        </section>

        <div class="two-col">
            <section class="card">
                <div class="section-head"><h3>Recent orders</h3><a href="${ctx}/admin/orders">All orders &rsaquo;</a></div>
                <table class="table">
                    <thead><tr><th>Order</th><th>Customer</th><th>Status</th><th class="num">Total</th></tr></thead>
                    <tbody>
                    <c:forEach var="o" items="${recentOrders}">
                        <tr>
                            <td><a href="${ctx}/admin/orders/${o.orderNumber}"><c:out value="${o.orderNumber}"/></a></td>
                            <td><c:out value="${o.customerName}"/></td>
                            <td><se:orderStatus status="${o.status}" label="${o.status.label}"/></td>
                            <td class="num"><se:money value="${o.total}"/></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty recentOrders}"><tr><td colspan="4" class="muted center">No orders yet.</td></tr></c:if>
                    </tbody>
                </table>
            </section>
            <section class="card">
                <div class="section-head"><h3>Low stock</h3><a href="${ctx}/admin/products">Inventory &rsaquo;</a></div>
                <table class="table">
                    <thead><tr><th>Product</th><th class="num">Stock</th></tr></thead>
                    <tbody>
                    <c:forEach var="p" items="${lowStock}">
                        <tr>
                            <td><a href="${ctx}/admin/products/${p.id}/edit"><c:out value="${p.name}"/></a></td>
                            <td class="num"><span class="${p.stock == 0 ? 'error-text' : 'warn-text'}">${p.stock}</span></td>
                        </tr>
                    </c:forEach>
                    <c:if test="${empty lowStock}"><tr><td colspan="2" class="muted center">All products are well stocked.</td></tr></c:if>
                    </tbody>
                </table>
            </section>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
