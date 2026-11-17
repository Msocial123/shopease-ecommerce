<%@ tag body-content="empty" pageEncoding="UTF-8" description="Product tile used in grids" %>
<%@ attribute name="product" required="true" type="com.shopease.model.Product" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="se" tagdir="/WEB-INF/tags" %>
<c:set var="base" value="${pageContext.request.contextPath}"/>
<article class="product-card">
    <a href="${base}/products/${product.id}" class="product-card-media">
        <se:productImage id="${product.id}" url="${product.imageUrl}" alt="${product.name}"/>
        <c:if test="${product.discountPercent > 0}"><span class="badge badge-sale">${product.discountPercent}% off</span></c:if>
        <c:if test="${!product.inStock}"><span class="badge badge-muted">Sold out</span></c:if>
    </a>
    <div class="product-card-body">
        <span class="product-brand"><c:out value="${product.brand}"/></span>
        <h3 class="product-title"><a href="${base}/products/${product.id}"><c:out value="${product.name}"/></a></h3>
        <se:rating value="${product.rating}"/>
        <div class="price-row">
            <span class="price"><se:money value="${product.price}"/></span>
            <c:if test="${product.discountPercent > 0}"><s class="mrp"><se:money value="${product.mrp}"/></s></c:if>
        </div>
        <c:choose>
            <c:when test="${product.inStock}">
                <form action="${base}/cart/add" method="post" class="js-add-to-cart">
                    <se:csrf/>
                    <input type="hidden" name="productId" value="${product.id}">
                    <input type="hidden" name="quantity" value="1">
                    <input type="hidden" name="returnTo" value="<c:out value='${requestScope.currentPath}'/>">
                    <button type="submit" class="btn btn-primary btn-block">Add to cart</button>
                </form>
            </c:when>
            <c:otherwise>
                <button type="button" class="btn btn-block" disabled>Out of stock</button>
            </c:otherwise>
        </c:choose>
    </div>
</article>
