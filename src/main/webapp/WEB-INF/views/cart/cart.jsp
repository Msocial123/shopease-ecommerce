<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Shopping cart"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container">
    <h1 class="page-title">Shopping cart <c:if test="${not empty cart.items}"><small class="muted">(${cart.itemCount} item${cart.itemCount == 1 ? '' : 's'})</small></c:if></h1>

    <c:choose>
        <c:when test="${empty cart.items}">
            <div class="empty-state card">
                <svg viewBox="0 0 24 24" width="64" height="64" aria-hidden="true"><path d="M3 4h2l2.4 11.2a2 2 0 0 0 2 1.6h7.7a2 2 0 0 0 2-1.5L21 8H6" fill="none" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" stroke-linejoin="round"/><circle cx="10" cy="20" r="1.2" fill="currentColor"/><circle cx="17" cy="20" r="1.2" fill="currentColor"/></svg>
                <h3>Your cart is empty</h3>
                <p class="muted">Looks like you haven't added anything yet.</p>
                <a class="btn btn-primary" href="${ctx}/products">Continue shopping</a>
            </div>
        </c:when>
        <c:otherwise>
            <div class="cart-layout">
                <section class="card cart-items">
                    <c:if test="${cart.freeShippingRemaining > 0}">
                        <div class="notice notice-info">Add <strong><se:money value="${cart.freeShippingRemaining}"/></strong> more to get <strong>FREE shipping</strong>.</div>
                    </c:if>
                    <c:forEach var="item" items="${cart.items}">
                        <div class="cart-row ${item.exceedsStock ? 'has-error' : ''}">
                            <a href="${ctx}/products/${item.product.id}" class="cart-thumb">
                                <se:productImage id="${item.product.id}" url="${item.product.imageUrl}" alt="${item.product.name}"/>
                            </a>
                            <div class="cart-info">
                                <a href="${ctx}/products/${item.product.id}" class="cart-name"><c:out value="${item.product.name}"/></a>
                                <span class="muted"><c:out value="${item.product.brand}"/> &middot; <se:money value="${item.product.price}"/> each</span>
                                <c:if test="${item.exceedsStock}">
                                    <span class="error-text">Only ${item.product.stock} left &mdash; please reduce the quantity.</span>
                                </c:if>
                                <div class="cart-actions">
                                    <form action="${ctx}/cart/update" method="post" class="inline-form">
                                        <se:csrf/>
                                        <input type="hidden" name="productId" value="${item.product.id}">
                                        <div class="qty-stepper qty-sm" data-qty data-autosubmit-qty>
                                            <button type="button" data-step="-1" aria-label="Decrease quantity">&minus;</button>
                                            <input type="number" name="quantity" value="${item.quantity}" min="0" max="10" aria-label="Quantity">
                                            <button type="button" data-step="1" aria-label="Increase quantity">+</button>
                                        </div>
                                        <noscript><button type="submit" class="btn btn-sm">Update</button></noscript>
                                    </form>
                                    <form action="${ctx}/cart/remove" method="post" class="inline-form">
                                        <se:csrf/>
                                        <input type="hidden" name="productId" value="${item.product.id}">
                                        <button type="submit" class="btn btn-link btn-danger">Remove</button>
                                    </form>
                                </div>
                            </div>
                            <div class="cart-line-total"><se:money value="${item.lineTotal}"/></div>
                        </div>
                    </c:forEach>
                    <div class="cart-footer">
                        <a href="${ctx}/products" class="btn btn-link">&lsaquo; Continue shopping</a>
                        <form action="${ctx}/cart/clear" method="post" data-confirm="Remove all items from your cart?">
                            <se:csrf/>
                            <button type="submit" class="btn btn-link btn-danger">Clear cart</button>
                        </form>
                    </div>
                </section>

                <aside class="card summary">
                    <h3>Order summary</h3>
                    <dl class="summary-list">
                        <dt>Subtotal</dt><dd><se:money value="${cart.subtotal}"/></dd>
                        <dt>GST (${applicationScope.taxPercent}%)</dt><dd><se:money value="${cart.tax}"/></dd>
                        <dt>Shipping</dt><dd><c:choose><c:when test="${cart.shipping.signum() == 0}"><span class="free">FREE</span></c:when><c:otherwise><se:money value="${cart.shipping}"/></c:otherwise></c:choose></dd>
                        <dt class="total">Total</dt><dd class="total"><se:money value="${cart.total}"/></dd>
                    </dl>
                    <c:if test="${cart.savings > 0}"><p class="save">You save <se:money value="${cart.savings}"/> on this order</p></c:if>
                    <c:choose>
                        <c:when test="${cart.hasStockIssues}"><button class="btn btn-primary btn-block btn-lg" disabled>Fix quantities to continue</button></c:when>
                        <c:otherwise><a class="btn btn-primary btn-block btn-lg" href="${ctx}/checkout">Proceed to checkout</a></c:otherwise>
                    </c:choose>
                    <c:if test="${empty sessionScope.currentUser}"><p class="muted small center">You'll be asked to log in or sign up first.</p></c:if>
                </aside>
            </div>
        </c:otherwise>
    </c:choose>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
