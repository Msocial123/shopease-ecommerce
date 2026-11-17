<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Manage products"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<c:url var="baseUrl" value="/admin/products">
    <c:if test="${not empty q}"><c:param name="q" value="${q}"/></c:if>
    <c:if test="${not empty category}"><c:param name="category" value="${category}"/></c:if>
</c:url>

<div class="container admin-layout">
    <%@ include file="/WEB-INF/views/common/admin-nav.jspf" %>
    <div class="admin-main">
        <div class="list-toolbar">
            <h1 class="page-title">Products <small class="muted">(${result.totalElements})</small></h1>
            <a class="btn btn-primary" href="${ctx}/admin/products/new">+ Add product</a>
        </div>
        <form class="card toolbar-form" method="get" action="${ctx}/admin/products">
            <input type="search" name="q" placeholder="Search name, SKU, brand" value="<c:out value='${q}'/>">
            <select name="category">
                <option value="">All categories</option>
                <c:forEach var="cat" items="${navCategories}">
                    <option value="${cat.id}" ${category == cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option>
                </c:forEach>
            </select>
            <button class="btn" type="submit">Filter</button>
        </form>

        <div class="card table-wrap">
            <table class="table">
                <thead><tr><th>Product</th><th>SKU</th><th>Category</th><th class="num">Price</th><th class="num">Stock</th><th>Status</th><th></th></tr></thead>
                <tbody>
                <c:forEach var="p" items="${result.content}">
                    <tr class="${p.active ? '' : 'row-muted'}">
                        <td class="item-cell">
                            <se:productImage id="${p.id}" url="${p.imageUrl}" alt="${p.name}" cssClass="thumb"/>
                            <div><a href="${ctx}/admin/products/${p.id}/edit"><c:out value="${p.name}"/></a>
                                <c:if test="${p.featured}"><span class="tag">Featured</span></c:if></div>
                        </td>
                        <td><code><c:out value="${p.sku}"/></code></td>
                        <td><c:out value="${p.categoryName}"/></td>
                        <td class="num"><se:money value="${p.price}"/></td>
                        <td class="num"><span class="${p.stock == 0 ? 'error-text' : (p.stock <= 10 ? 'warn-text' : '')}">${p.stock}</span></td>
                        <td><span class="status ${p.active ? 'status-PAID' : 'status-CANCELLED'}">${p.active ? 'Live' : 'Hidden'}</span></td>
                        <td class="actions">
                            <a class="btn btn-sm" href="${ctx}/admin/products/${p.id}/edit">Edit</a>
                            <form action="${ctx}/admin/products/${p.id}/toggle" method="post" class="inline-form">
                                <se:csrf/>
                                <button class="btn btn-sm btn-link" type="submit">${p.active ? 'Hide' : 'Publish'}</button>
                            </form>
                        </td>
                    </tr>
                </c:forEach>
                <c:if test="${empty result.content}"><tr><td colspan="7" class="center muted">No products match.</td></tr></c:if>
                </tbody>
            </table>
        </div>
        <se:pagination pager="${result}" url="${baseUrl}"/>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
