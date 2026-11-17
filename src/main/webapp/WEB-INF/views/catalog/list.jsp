<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:choose>
    <c:when test="${not empty selectedCategory}"><c:set var="pageTitle" value="${selectedCategory.name}"/></c:when>
    <c:when test="${not empty q}"><c:set var="pageTitle" value="Results for ${q}"/></c:when>
    <c:otherwise><c:set var="pageTitle" value="All products"/></c:otherwise>
</c:choose>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<%-- Base URL (without page=) used by sort links and the pager --%>
<c:url var="baseUrl" value="${empty selectedCategory ? '/products' : '/category/'.concat(selectedCategory.slug)}">
    <c:if test="${not empty q and empty selectedCategory}"><c:param name="q" value="${q}"/></c:if>
    <c:if test="${not empty min}"><c:param name="min" value="${min}"/></c:if>
    <c:if test="${not empty max}"><c:param name="max" value="${max}"/></c:if>
    <c:if test="${inStock}"><c:param name="inStock" value="on"/></c:if>
    <c:param name="sort" value="${sort}"/>
</c:url>

<div class="container">
    <nav class="breadcrumb" aria-label="Breadcrumb">
        <a href="${ctx}/">Home</a> <span>/</span>
        <c:choose>
            <c:when test="${not empty selectedCategory}"><a href="${ctx}/products">Products</a> <span>/</span> <c:out value="${selectedCategory.name}"/></c:when>
            <c:otherwise>Products</c:otherwise>
        </c:choose>
    </nav>

    <div class="catalog-layout">
        <aside class="filters card">
            <form method="get" action="${ctx}${empty selectedCategory ? '/products' : '/category/'.concat(selectedCategory.slug)}">
                <c:if test="${empty selectedCategory}">
                    <label class="field">
                        <span>Search</span>
                        <input type="search" name="q" value="<c:out value='${q}'/>" placeholder="Keyword">
                    </label>
                </c:if>
                <div class="field">
                    <span>Category</span>
                    <div class="filter-links">
                        <a href="${ctx}/products" class="${empty selectedCategory ? 'active' : ''}">All</a>
                        <c:forEach var="cat" items="${navCategories}">
                            <a href="${ctx}/category/${cat.slug}" class="${selectedCategory.id == cat.id ? 'active' : ''}">
                                <c:out value="${cat.name}"/> <small>(${cat.productCount})</small></a>
                        </c:forEach>
                    </div>
                </div>
                <div class="field">
                    <span>Price range</span>
                    <div class="row-2">
                        <input type="number" name="min" min="0" step="1" placeholder="Min" value="${min}">
                        <input type="number" name="max" min="0" step="1" placeholder="Max" value="${max}">
                    </div>
                </div>
                <label class="check">
                    <input type="checkbox" name="inStock" value="on" ${inStock ? 'checked' : ''}> In stock only
                </label>
                <input type="hidden" name="sort" value="<c:out value='${sort}'/>">
                <button class="btn btn-primary btn-block" type="submit">Apply filters</button>
                <a class="btn btn-link btn-block" href="${ctx}${empty selectedCategory ? '/products' : '/category/'.concat(selectedCategory.slug)}">Clear</a>
            </form>
        </aside>

        <section>
            <div class="list-toolbar">
                <div>
                    <h1 class="page-title"><c:out value="${pageTitle}"/></h1>
                    <p class="muted">
                        <c:choose>
                            <c:when test="${result.totalElements == 0}">No products found</c:when>
                            <c:otherwise>Showing ${result.firstItem}&ndash;${result.lastItem} of ${result.totalElements} products</c:otherwise>
                        </c:choose>
                    </p>
                </div>
                <form method="get" class="sort-form" action="${ctx}${empty selectedCategory ? '/products' : '/category/'.concat(selectedCategory.slug)}">
                    <c:if test="${not empty q and empty selectedCategory}"><input type="hidden" name="q" value="<c:out value='${q}'/>"></c:if>
                    <c:if test="${not empty min}"><input type="hidden" name="min" value="${min}"></c:if>
                    <c:if test="${not empty max}"><input type="hidden" name="max" value="${max}"></c:if>
                    <c:if test="${inStock}"><input type="hidden" name="inStock" value="on"></c:if>
                    <label>Sort by
                        <select name="sort" data-autosubmit>
                            <option value="relevance" ${sort == 'relevance' ? 'selected' : ''}>Relevance</option>
                            <option value="newest" ${sort == 'newest' ? 'selected' : ''}>Newest first</option>
                            <option value="price_asc" ${sort == 'price_asc' ? 'selected' : ''}>Price: low to high</option>
                            <option value="price_desc" ${sort == 'price_desc' ? 'selected' : ''}>Price: high to low</option>
                            <option value="rating" ${sort == 'rating' ? 'selected' : ''}>Customer rating</option>
                            <option value="name" ${sort == 'name' ? 'selected' : ''}>Name A&ndash;Z</option>
                        </select>
                    </label>
                    <noscript><button class="btn btn-sm" type="submit">Go</button></noscript>
                </form>
            </div>

            <c:choose>
                <c:when test="${empty result.content}">
                    <div class="empty-state card">
                        <h3>We couldn't find anything matching your search</h3>
                        <p class="muted">Try a different keyword or remove some filters.</p>
                        <a class="btn btn-primary" href="${ctx}/products">Browse all products</a>
                    </div>
                </c:when>
                <c:otherwise>
                    <div class="product-grid">
                        <c:forEach var="p" items="${result.content}">
                            <se:productCard product="${p}"/>
                        </c:forEach>
                    </div>
                    <se:pagination pager="${result}" url="${baseUrl}"/>
                </c:otherwise>
            </c:choose>
        </section>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
