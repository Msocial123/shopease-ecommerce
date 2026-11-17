<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Order ${order.orderNumber}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container">
    <c:if test="${placed}">
        <div class="confirmation card">
            <div class="confirm-icon">&#10003;</div>
            <h1>Thank you for your order!</h1>
            <p>Your order <strong><c:out value="${order.orderNumber}"/></strong> has been placed successfully.
                <c:if test="${not empty sessionScope.currentUser}">We'll keep you posted at <c:out value="${sessionScope.currentUser.email}"/>.</c:if></p>
        </div>
    </c:if>

    <nav class="breadcrumb" aria-label="Breadcrumb"><a href="${ctx}/orders">My orders</a> <span>/</span> <c:out value="${order.orderNumber}"/></nav>

    <div class="order-head">
        <div>
            <h1 class="page-title">Order <c:out value="${order.orderNumber}"/></h1>
            <p class="muted">Placed on <fmt:formatDate value="${order.createdDate}" pattern="dd MMM yyyy, hh:mm a"/> &middot; <c:out value="${order.paymentMethod.label}"/></p>
        </div>
        <div class="order-head-actions">
            <se:orderStatus status="${order.status}" label="${order.status.label}"/>
            <c:if test="${order.status.awaitingPayment}"><a class="btn btn-primary" href="${ctx}/payment/${order.orderNumber}">Complete payment</a></c:if>
            <c:if test="${order.status.cancellable}">
                <form action="${ctx}/orders/${order.orderNumber}/cancel" method="post" data-confirm="Are you sure you want to cancel this order?">
                    <se:csrf/>
                    <button type="submit" class="btn btn-outline btn-danger">Cancel order</button>
                </form>
            </c:if>
        </div>
    </div>

    <c:if test="${order.status.name != 'CANCELLED'}">
        <ol class="tracker">
            <li class="${order.status.name != 'PENDING_PAYMENT' ? 'done' : 'current'}">Ordered</li>
            <li class="${order.status.name == 'SHIPPED' || order.status.name == 'DELIVERED' ? 'done' : (order.status.name == 'PAID' || order.status.name == 'PLACED' ? 'current' : '')}">Confirmed</li>
            <li class="${order.status.name == 'DELIVERED' ? 'done' : (order.status.name == 'SHIPPED' ? 'current' : '')}">Shipped</li>
            <li class="${order.status.name == 'DELIVERED' ? 'done' : ''}">Delivered</li>
        </ol>
    </c:if>

    <%@ include file="/WEB-INF/views/common/order-body.jspf" %>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
