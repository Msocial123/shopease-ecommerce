<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Online shopping for electronics, fashion, home and more"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<section class="hero">
    <div class="container hero-inner">
        <div class="hero-copy">
            <span class="eyebrow">Mega sale is live</span>
            <h1>Everything you love,<br>delivered to your door.</h1>
            <p>Shop top brands across electronics, fashion, home, books and more &mdash; with free shipping on orders over
                <se:money value="${applicationScope.freeShippingThreshold}"/>.</p>
            <div class="hero-actions">
                <a class="btn btn-light btn-lg" href="${ctx}/products">Start shopping</a>
                <a class="btn btn-ghost btn-lg" href="${ctx}/products?sort=newest">New arrivals</a>
            </div>
        </div>
        <div class="hero-art" aria-hidden="true">
            <div class="hero-card hc-1">Up to 50% off</div>
            <div class="hero-card hc-2">Free delivery</div>
            <div class="hero-card hc-3">Secure checkout</div>
        </div>
    </div>
</section>

<div class="container">
    <section class="section">
        <div class="section-head">
            <h2>Shop by category</h2>
            <a href="${ctx}/products">View all &rsaquo;</a>
        </div>
        <div class="category-grid">
            <c:forEach var="cat" items="${navCategories}" varStatus="s">
                <a class="category-tile cat-${s.index % 6}" href="${ctx}/category/${cat.slug}">
                    <strong><c:out value="${cat.name}"/></strong>
                    <span><c:out value="${cat.description}"/></span>
                    <em>${cat.productCount} products</em>
                </a>
            </c:forEach>
        </div>
    </section>

    <section class="section">
        <div class="section-head">
            <h2>Featured deals</h2>
            <a href="${ctx}/products">See more &rsaquo;</a>
        </div>
        <div class="product-grid">
            <c:forEach var="p" items="${featured}">
                <se:productCard product="${p}"/>
            </c:forEach>
        </div>
    </section>

    <section class="perks">
        <div class="perk"><strong>Free shipping</strong><span>On orders over <se:money value="${applicationScope.freeShippingThreshold}"/></span></div>
        <div class="perk"><strong>Secure payments</strong><span>Cards, UPI, net banking &amp; COD</span></div>
        <div class="perk"><strong>Easy cancellations</strong><span>Cancel any time before shipping</span></div>
        <div class="perk"><strong>Genuine products</strong><span>Sourced directly from brands</span></div>
    </section>

    <section class="section">
        <div class="section-head">
            <h2>New arrivals</h2>
            <a href="${ctx}/products?sort=newest">See more &rsaquo;</a>
        </div>
        <div class="product-grid">
            <c:forEach var="p" items="${newest}">
                <se:productCard product="${p}"/>
            </c:forEach>
        </div>
    </section>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
