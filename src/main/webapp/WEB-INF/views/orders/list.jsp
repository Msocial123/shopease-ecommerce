<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="My orders"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container">
    <h1 class="page-title">My orders</h1>
    <c:choose>
        <c:when test="${empty orders}">
            <div class="empty-state card">
                <h3>No orders yet</h3>
                <p class="muted">When you place an order it will show up here.</p>
                <a class="btn btn-primary" href="${ctx}/products">Start shopping</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="order-list">
                <c:forEach var="o" items="${orders}">
                    <a class="order-card card" href="${ctx}/orders/${o.orderNumber}">
                        <div>
                            <strong><c:out value="${o.orderNumber}"/></strong>
                            <span class="muted"><fmt:formatDate value="${o.createdDate}" pattern="dd MMM yyyy, hh:mm a"/></span>
                        </div>
                        <div class="muted">${o.itemCount} item${o.itemCount == 1 ? '' : 's'} &middot; <c:out value="${o.paymentMethod.label}"/></div>
                        <div><se:orderStatus status="${o.status}" label="${o.status.label}"/></div>
                        <div class="order-total"><se:money value="${o.total}"/></div>
                        <c:if test="${o.status.awaitingPayment}"><span class="btn btn-sm btn-primary">Complete payment</span></c:if>
                    </a>
                </c:forEach>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
