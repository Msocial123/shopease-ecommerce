<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="About us"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container narrow">
    <div class="card prose">
        <h1>About <c:out value="${applicationScope.storeName}"/></h1>
        <p><c:out value="${applicationScope.storeName}"/> is a full-featured online store built on the classic Java web stack:
            Jakarta Servlet 6.1, JSP with JSTL 3, Spring MVC and MySQL, running on Apache Tomcat 11.</p>
        <h3>How it is organised</h3>
        <ul>
            <li><strong>Catalogue service</strong> &mdash; categories, product search, filters, sorting and pagination.</li>
            <li><strong>Cart service</strong> &mdash; session cart for guests, persistent cart for members, merged at login.</li>
            <li><strong>Orders service</strong> &mdash; transactional checkout with stock reservation, history and cancellation.</li>
            <li><strong>Payment service</strong> &mdash; cards, UPI, net banking and cash on delivery through a simulated gateway.</li>
        </ul>
        <h3>Shipping &amp; taxes</h3>
        <p>Orders of <se:money value="${applicationScope.freeShippingThreshold}"/> or more ship free; smaller orders carry a flat
            <se:money value="${applicationScope.shippingFee}"/> fee. GST of ${applicationScope.taxPercent}% is added at checkout.</p>
        <p><a class="btn btn-primary" href="${ctx}/products">Browse the catalogue</a></p>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
