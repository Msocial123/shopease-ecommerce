<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Payment"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container">
    <ol class="steps">
        <li class="done">Cart</li>
        <li class="done">Shipping</li>
        <li class="current">Payment</li>
        <li>Confirmation</li>
    </ol>

    <div class="checkout-layout">
        <section class="card">
            <h2>Choose how to pay</h2>
            <p class="muted small">Order <strong><c:out value="${order.orderNumber}"/></strong> &middot; stock is reserved for you.</p>

            <form action="${ctx}/payment/${order.orderNumber}" method="post" class="payment-form" data-payment-form autocomplete="off">
                <se:csrf/>
                <div class="pay-tabs" role="radiogroup" aria-label="Payment method">
                    <c:forEach var="m" items="${paymentMethods}">
                        <label class="pay-tab">
                            <input type="radio" name="method" value="${m.name}" ${order.paymentMethod == m ? 'checked' : ''} data-pay-method>
                            <span><c:out value="${m.label}"/></span>
                        </label>
                    </c:forEach>
                </div>

                <fieldset class="pay-panel" data-panel="CARD">
                    <legend class="sr-only">Card details</legend>
                    <div class="form-grid">
                        <label class="field span-2">
                            <span>Card number</span>
                            <input type="text" name="cardNumber" inputmode="numeric" maxlength="23" placeholder="1234 5678 9012 3456" data-card-number autocomplete="cc-number">
                        </label>
                        <label class="field span-2">
                            <span>Name on card</span>
                            <input type="text" name="cardName" maxlength="80" autocomplete="cc-name">
                        </label>
                        <label class="field">
                            <span>Expiry (MM/YY)</span>
                            <input type="text" name="cardExpiry" inputmode="numeric" maxlength="5" placeholder="MM/YY" data-card-expiry autocomplete="cc-exp">
                        </label>
                        <label class="field">
                            <span>CVV</span>
                            <input type="password" name="cardCvv" inputmode="numeric" maxlength="4" placeholder="&bull;&bull;&bull;" autocomplete="cc-csc">
                        </label>
                    </div>
                    <p class="hint">Demo gateway &mdash; use <code>4111 1111 1111 1111</code> with any future expiry and CVV <code>123</code>.
                        A card ending in <code>0002</code> is declined.</p>
                </fieldset>

                <fieldset class="pay-panel" data-panel="UPI">
                    <legend class="sr-only">UPI</legend>
                    <label class="field">
                        <span>UPI ID</span>
                        <input type="text" name="upiId" maxlength="100" placeholder="yourname@okbank">
                    </label>
                    <p class="hint">Demo gateway &mdash; any UPI ID succeeds except <code>fail@upi</code>.</p>
                </fieldset>

                <fieldset class="pay-panel" data-panel="NETBANKING">
                    <legend class="sr-only">Net banking</legend>
                    <label class="field">
                        <span>Select your bank</span>
                        <select name="bank">
                            <option value="">&mdash; Choose bank &mdash;</option>
                            <c:forEach var="b" items="${banks}"><option value="<c:out value='${b}'/>"><c:out value="${b}"/></option></c:forEach>
                        </select>
                    </label>
                </fieldset>

                <fieldset class="pay-panel" data-panel="COD">
                    <legend class="sr-only">Cash on delivery</legend>
                    <c:choose>
                        <c:when test="${order.total > codLimit}">
                            <div class="notice notice-error">Cash on Delivery is available only for orders up to <se:money value="${codLimit}"/>.</div>
                        </c:when>
                        <c:otherwise>
                            <div class="notice notice-info">Pay <strong><se:money value="${order.total}"/></strong> in cash or UPI when your order arrives.</div>
                        </c:otherwise>
                    </c:choose>
                </fieldset>

                <button type="submit" class="btn btn-primary btn-lg btn-block" data-pay-button>
                    Pay <se:money value="${order.total}"/>
                </button>
                <p class="secure-note">&#128274; Card details are sent over HTTPS and never stored &mdash; only the last 4 digits are kept.</p>
            </form>

            <form action="${ctx}/orders/${order.orderNumber}/cancel" method="post" data-confirm="Cancel this order? Reserved stock will be released.">
                <se:csrf/>
                <button type="submit" class="btn btn-link btn-danger">Cancel order</button>
            </form>
        </section>

        <aside class="card summary">
            <h3>Order <c:out value="${order.orderNumber}"/></h3>
            <ul class="mini-items">
                <c:forEach var="it" items="${order.items}">
                    <li>
                        <se:productImage id="${it.productId}" url="${it.imageUrl}" alt="${it.productName}"/>
                        <span class="mini-name"><c:out value="${it.productName}"/> <small class="muted">&times; ${it.quantity}</small></span>
                        <span><se:money value="${it.lineTotal}"/></span>
                    </li>
                </c:forEach>
            </ul>
            <dl class="summary-list">
                <dt>Subtotal</dt><dd><se:money value="${order.subtotal}"/></dd>
                <dt>GST</dt><dd><se:money value="${order.tax}"/></dd>
                <dt>Shipping</dt><dd><se:money value="${order.shipping}"/></dd>
                <dt class="total">Amount payable</dt><dd class="total"><se:money value="${order.total}"/></dd>
            </dl>
            <div class="ship-to">
                <strong>Deliver to</strong>
                <span><c:out value="${order.shipName}"/>, <c:out value="${order.shipPhone}"/></span>
                <span><c:out value="${order.shipLine1}"/><c:if test="${not empty order.shipLine2}">, <c:out value="${order.shipLine2}"/></c:if></span>
                <span><c:out value="${order.shipCity}"/>, <c:out value="${order.shipState}"/> - <c:out value="${order.shipZip}"/></span>
            </div>
        </aside>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
