<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Order ${order.orderNumber}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container admin-layout">
    <%@ include file="/WEB-INF/views/common/admin-nav.jspf" %>
    <div class="admin-main">
        <nav class="breadcrumb"><a href="${ctx}/admin/orders">Orders</a> <span>/</span> <c:out value="${order.orderNumber}"/></nav>
        <div class="order-head">
            <div>
                <h1 class="page-title">Order <c:out value="${order.orderNumber}"/></h1>
                <p class="muted"><fmt:formatDate value="${order.createdDate}" pattern="dd MMM yyyy, hh:mm a"/> &middot;
                    <c:out value="${order.customerName}"/> (<c:out value="${order.customerEmail}"/>) &middot; <c:out value="${order.paymentMethod.label}"/></p>
            </div>
            <div class="order-head-actions">
                <se:orderStatus status="${order.status}" label="${order.status.label}"/>
                <c:if test="${order.status.name == 'PAID' || order.status.name == 'PLACED'}">
                    <form action="${ctx}/admin/orders/${order.orderNumber}/status" method="post" class="inline-form">
                        <se:csrf/><input type="hidden" name="status" value="SHIPPED">
                        <button class="btn btn-primary" type="submit">Mark as shipped</button>
                    </form>
                </c:if>
                <c:if test="${order.status.name == 'SHIPPED'}">
                    <form action="${ctx}/admin/orders/${order.orderNumber}/status" method="post" class="inline-form">
                        <se:csrf/><input type="hidden" name="status" value="DELIVERED">
                        <button class="btn btn-primary" type="submit">Mark as delivered</button>
                    </form>
                </c:if>
                <c:if test="${order.status.cancellable}">
                    <form action="${ctx}/admin/orders/${order.orderNumber}/status" method="post" class="inline-form"
                          data-confirm="Cancel this order, restock items and refund any payment?">
                        <se:csrf/><input type="hidden" name="status" value="CANCELLED">
                        <button class="btn btn-outline btn-danger" type="submit">Cancel order</button>
                    </form>
                </c:if>
            </div>
        </div>

        <%@ include file="/WEB-INF/views/common/order-body.jspf" %>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
