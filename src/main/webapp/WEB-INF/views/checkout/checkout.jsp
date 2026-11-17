<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Checkout"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container">
    <ol class="steps">
        <li class="done">Cart</li>
        <li class="current">Shipping</li>
        <li>Payment</li>
        <li>Confirmation</li>
    </ol>

    <form action="${ctx}/checkout" method="post" class="checkout-layout" novalidate>
        <se:csrf/>
        <section class="card">
            <h2>Shipping address</h2>
            <c:if test="${not empty error}"><div class="notice notice-error"><c:out value="${error}"/></div></c:if>
            <div class="form-grid">
                <label class="field">
                    <span>Full name *</span>
                    <input type="text" name="name" required maxlength="120" value="<c:out value='${address.name}'/>" autocomplete="name">
                </label>
                <label class="field">
                    <span>Mobile number *</span>
                    <input type="tel" name="phone" required maxlength="15" pattern="[0-9+\- ]{7,15}" value="<c:out value='${address.phone}'/>" autocomplete="tel">
                </label>
                <label class="field span-2">
                    <span>Address line 1 *</span>
                    <input type="text" name="line1" required maxlength="200" placeholder="House no., building, street" value="<c:out value='${address.line1}'/>" autocomplete="address-line1">
                </label>
                <label class="field span-2">
                    <span>Address line 2</span>
                    <input type="text" name="line2" maxlength="200" placeholder="Area, landmark (optional)" value="<c:out value='${address.line2}'/>" autocomplete="address-line2">
                </label>
                <label class="field">
                    <span>City *</span>
                    <input type="text" name="city" required maxlength="80" value="<c:out value='${address.city}'/>" autocomplete="address-level2">
                </label>
                <label class="field">
                    <span>State *</span>
                    <input type="text" name="state" required maxlength="80" value="<c:out value='${address.state}'/>" autocomplete="address-level1">
                </label>
                <label class="field">
                    <span>PIN / ZIP code *</span>
                    <input type="text" name="zip" required maxlength="10" value="<c:out value='${address.zip}'/>" autocomplete="postal-code">
                </label>
                <label class="field span-2">
                    <span>Delivery notes</span>
                    <textarea name="notes" rows="2" maxlength="500" placeholder="Any instructions for the delivery partner"><c:out value="${address.notes}"/></textarea>
                </label>
            </div>

            <h2>Preferred payment method</h2>
            <p class="muted small">You'll enter payment details on the next step.</p>
            <div class="method-grid">
                <c:forEach var="m" items="${paymentMethods}" varStatus="s">
                    <label class="method-option">
                        <input type="radio" name="paymentMethod" value="${m.name}"
                            ${(empty selectedMethod and s.first) or selectedMethod == m ? 'checked' : ''}>
                        <span><c:out value="${m.label}"/></span>
                    </label>
                </c:forEach>
            </div>
        </section>

        <aside class="card summary">
            <h3>Order summary</h3>
            <ul class="mini-items">
                <c:forEach var="item" items="${cart.items}">
                    <li>
                        <se:productImage id="${item.product.id}" url="${item.product.imageUrl}" alt="${item.product.name}"/>
                        <span class="mini-name"><c:out value="${item.product.name}"/> <small class="muted">&times; ${item.quantity}</small></span>
                        <span><se:money value="${item.lineTotal}"/></span>
                    </li>
                </c:forEach>
            </ul>
            <dl class="summary-list">
                <dt>Subtotal</dt><dd><se:money value="${cart.subtotal}"/></dd>
                <dt>GST (${applicationScope.taxPercent}%)</dt><dd><se:money value="${cart.tax}"/></dd>
                <dt>Shipping</dt><dd><c:choose><c:when test="${cart.shipping.signum() == 0}"><span class="free">FREE</span></c:when><c:otherwise><se:money value="${cart.shipping}"/></c:otherwise></c:choose></dd>
                <dt class="total">Total</dt><dd class="total"><se:money value="${cart.total}"/></dd>
            </dl>
            <button type="submit" class="btn btn-primary btn-block btn-lg">Continue to payment</button>
            <a class="btn btn-link btn-block" href="${ctx}/cart">&lsaquo; Back to cart</a>
        </aside>
    </form>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
