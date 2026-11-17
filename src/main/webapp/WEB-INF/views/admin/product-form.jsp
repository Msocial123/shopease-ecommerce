<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="${empty product.id ? 'Add product' : 'Edit product'}"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container admin-layout">
    <%@ include file="/WEB-INF/views/common/admin-nav.jspf" %>
    <div class="admin-main">
        <nav class="breadcrumb"><a href="${ctx}/admin/products">Products</a> <span>/</span> <c:out value="${pageTitle}"/></nav>
        <h1 class="page-title"><c:out value="${pageTitle}"/></h1>
        <c:if test="${not empty error}"><div class="notice notice-error"><c:out value="${error}"/></div></c:if>

        <form action="${ctx}/admin/products/save" method="post" class="card product-form">
            <se:csrf/>
            <c:if test="${not empty product.id}"><input type="hidden" name="id" value="${product.id}"></c:if>
            <div class="form-grid">
                <label class="field span-2"><span>Product name *</span>
                    <input type="text" name="name" required maxlength="160" value="<c:out value='${product.name}'/>"></label>
                <label class="field"><span>SKU *</span>
                    <input type="text" name="sku" required maxlength="40" value="<c:out value='${product.sku}'/>"></label>
                <label class="field"><span>Brand</span>
                    <input type="text" name="brand" maxlength="80" value="<c:out value='${product.brand}'/>"></label>
                <label class="field"><span>Category *</span>
                    <select name="categoryId" required>
                        <option value="">&mdash; Select &mdash;</option>
                        <c:forEach var="cat" items="${navCategories}">
                            <option value="${cat.id}" ${product.categoryId == cat.id ? 'selected' : ''}><c:out value="${cat.name}"/></option>
                        </c:forEach>
                    </select></label>
                <label class="field"><span>Rating (0&ndash;5)</span>
                    <input type="number" name="rating" min="0" max="5" step="0.1" value="${product.rating}"></label>
                <label class="field"><span>Selling price *</span>
                    <input type="number" name="price" required min="0.01" step="0.01" value="${product.price}"></label>
                <label class="field"><span>MRP</span>
                    <input type="number" name="mrp" min="0.01" step="0.01" value="${product.mrp}"></label>
                <label class="field"><span>Stock *</span>
                    <input type="number" name="stock" required min="0" step="1" value="${product.stock}"></label>
                <label class="field"><span>Image URL</span>
                    <input type="text" name="imageUrl" maxlength="500" placeholder="https://... (leave empty for auto image)" value="<c:out value='${product.imageUrl}'/>"></label>
                <label class="field span-2"><span>Description</span>
                    <textarea name="description" rows="5"><c:out value="${product.description}"/></textarea></label>
                <label class="check"><input type="hidden" name="_featured" value="on">
                    <input type="checkbox" name="featured" ${product.featured ? 'checked' : ''}> Featured on home page</label>
                <label class="check"><input type="hidden" name="_active" value="on">
                    <input type="checkbox" name="active" ${product.active ? 'checked' : ''}> Visible in store</label>
            </div>
            <div class="form-actions">
                <button type="submit" class="btn btn-primary">Save product</button>
                <a class="btn btn-link" href="${ctx}/admin/products">Cancel</a>
            </div>
        </form>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
