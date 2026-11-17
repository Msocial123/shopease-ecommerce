<%@ include file="/WEB-INF/views/common/taglibs.jspf" %>
<c:set var="pageTitle" value="Manage categories"/>
<%@ include file="/WEB-INF/views/common/header.jspf" %>

<div class="container admin-layout">
    <%@ include file="/WEB-INF/views/common/admin-nav.jspf" %>
    <div class="admin-main">
        <h1 class="page-title">Categories</h1>
        <div class="two-col">
            <section class="card table-wrap">
                <table class="table">
                    <thead><tr><th>Name</th><th>Slug</th><th class="num">Products</th><th></th></tr></thead>
                    <tbody>
                    <c:forEach var="cat" items="${categories}">
                        <tr>
                            <td><strong><c:out value="${cat.name}"/></strong><div class="muted small"><c:out value="${cat.description}"/></div></td>
                            <td><code><c:out value="${cat.slug}"/></code></td>
                            <td class="num">${cat.productCount}</td>
                            <td class="actions">
                                <a class="btn btn-sm" href="${ctx}/admin/categories/${cat.id}/edit">Edit</a>
                                <form action="${ctx}/admin/categories/${cat.id}/delete" method="post" class="inline-form" data-confirm="Delete this category?">
                                    <se:csrf/>
                                    <button type="submit" class="btn btn-sm btn-link btn-danger">Delete</button>
                                </form>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
            </section>
            <section class="card">
                <h3>${empty category.id ? 'Add category' : 'Edit category'}</h3>
                <form action="${ctx}/admin/categories/save" method="post" class="stack">
                    <se:csrf/>
                    <c:if test="${not empty category.id}"><input type="hidden" name="id" value="${category.id}"></c:if>
                    <label class="field"><span>Name *</span>
                        <input type="text" name="name" required maxlength="80" value="<c:out value='${category.name}'/>"></label>
                    <label class="field"><span>Slug <small class="muted">(auto from name if empty)</small></span>
                        <input type="text" name="slug" maxlength="80" value="<c:out value='${category.slug}'/>"></label>
                    <label class="field"><span>Description</span>
                        <textarea name="description" rows="3" maxlength="500"><c:out value="${category.description}"/></textarea></label>
                    <button type="submit" class="btn btn-primary">Save category</button>
                    <c:if test="${not empty category.id}"><a class="btn btn-link" href="${ctx}/admin/categories">Cancel</a></c:if>
                </form>
            </section>
        </div>
    </div>
</div>

<%@ include file="/WEB-INF/views/common/footer.jspf" %>
