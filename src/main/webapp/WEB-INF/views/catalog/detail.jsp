<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="${product.name}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container">
    <nav class="breadcrumb" aria-label="Breadcrumb">
        <a href="${ctx}/">Home</a> <span>/</span>
        <a href="${ctx}/category/${product.categorySlug}"><c:out value="${product.categoryName}"/></a> <span>/</span>
        <c:out value="${product.name}"/>
    </nav>

    <div class="product-detail card">
        <div class="product-gallery">
            <se:productImage id="${product.id}" url="${product.imageUrl}" alt="${product.name}"/>
            <c:if test="${product.discountPercent > 0}"><span class="badge badge-sale badge-lg">${product.discountPercent}% off</span></c:if>
        </div>
        <div class="product-info">
            <span class="product-brand"><c:out value="${product.brand}"/></span>
            <h1><c:out value="${product.name}"/></h1>
            <div class="meta-row">
                <se:rating value="${product.rating}"/>
                <span class="muted">SKU: <c:out value="${product.sku}"/></span>
            </div>

            <div class="price-block">
                <span class="price price-lg"><se:money value="${product.price}"/></span>
                <c:if test="${product.discountPercent > 0}">
                    <s class="mrp">MRP <se:money value="${product.mrp}"/></s>
                    <span class="save">You save <se:money value="${product.mrp - product.price}"/></span>
                </c:if>
                <small class="muted">Inclusive of all offers. GST (${applicationScope.taxPercent}%) added at checkout.</small>
            </div>

            <c:choose>
                <c:when test="${!product.inStock}"><p class="stock stock-out">Currently out of stock</p></c:when>
                <c:when test="${product.lowStock}"><p class="stock stock-low">Hurry, only ${product.stock} left in stock!</p></c:when>
                <c:otherwise><p class="stock stock-in">In stock &middot; ready to ship</p></c:otherwise>
            </c:choose>

            <c:if test="${product.inStock}">
                <form action="${ctx}/cart/add" method="post" class="buy-form js-add-to-cart">
                    <se:csrf/>
                    <input type="hidden" name="productId" value="${product.id}">
                    <input type="hidden" name="returnTo" value="<c:out value='${currentPath}'/>">
                    <div class="qty-stepper" data-qty>
                        <button type="button" data-step="-1" aria-label="Decrease quantity">&minus;</button>
                        <input type="number" name="quantity" value="1" min="1" max="${product.stock < 10 ? product.stock : 10}" aria-label="Quantity">
                        <button type="button" data-step="1" aria-label="Increase quantity">+</button>
                    </div>
                    <button type="submit" class="btn btn-primary btn-lg">Add to cart</button>
                </form>
                <form action="${ctx}/cart/add" method="post" class="buy-now-form">
                    <se:csrf/>
                    <input type="hidden" name="productId" value="${product.id}">
                    <input type="hidden" name="quantity" value="1">
                    <input type="hidden" name="returnTo" value="/checkout">
                    <button type="submit" class="btn btn-accent btn-lg btn-block">Buy now</button>
                </form>
            </c:if>

            <ul class="assurance">
                <li>Free delivery on orders over <se:money value="${applicationScope.freeShippingThreshold}"/></li>
                <li>Cancel any time before the order ships</li>
                <li>Pay by card, UPI, net banking or cash on delivery</li>
            </ul>

            <h3>About this item</h3>
            <p class="description"><c:out value="${product.description}"/></p>
        </div>
    </div>

    <c:if test="${not empty related}">
        <section class="section">
            <div class="section-head">
                <h2>You may also like</h2>
                <a href="${ctx}/category/${product.categorySlug}">More in <c:out value="${product.categoryName}"/> &rsaquo;</a>
            </div>
            <div class="product-grid">
                <c:forEach var="p" items="${related}">
                    <se:productCard product="${p}"/>
                </c:forEach>
            </div>
        </section>
    </c:if>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
