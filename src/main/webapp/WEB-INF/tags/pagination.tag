<%@ tag body-content="empty" pageEncoding="UTF-8" description="Pager. url = base URL (already context-prefixed, may contain a query string)" %>
<%@ attribute name="pager" required="true" type="com.shopease.model.Page" %>
<%@ attribute name="url" required="true" type="java.lang.String" %>
<%@ taglib prefix="c" uri="jakarta.tags.core" %>
<%@ taglib prefix="fn" uri="jakarta.tags.functions" %>
<c:set var="sep" value="${fn:contains(url, '?') ? '&' : '?'}"/>
<c:if test="${pager.totalPages > 1}">
    <nav class="pagination" aria-label="Pagination">
        <c:choose>
            <c:when test="${pager.hasPrevious}"><a href="<c:out value='${url}${sep}page=${pager.page - 1}'/>">&lsaquo; Prev</a></c:when>
            <c:otherwise><span class="disabled">&lsaquo; Prev</span></c:otherwise>
        </c:choose>
        <c:forEach var="i" begin="1" end="${pager.totalPages}">
            <c:if test="${i == 1 || i == pager.totalPages || (i >= pager.page - 2 && i <= pager.page + 2)}">
                <c:choose>
                    <c:when test="${i == pager.page}"><span class="current" aria-current="page">${i}</span></c:when>
                    <c:otherwise><a href="<c:out value='${url}${sep}page=${i}'/>">${i}</a></c:otherwise>
                </c:choose>
            </c:if>
            <c:if test="${(i == 2 && pager.page > 4) || (i == pager.totalPages - 1 && pager.page < pager.totalPages - 3)}"><span class="gap">&hellip;</span></c:if>
        </c:forEach>
        <c:choose>
            <c:when test="${pager.hasNext}"><a href="<c:out value='${url}${sep}page=${pager.page + 1}'/>">Next &rsaquo;</a></c:when>
            <c:otherwise><span class="disabled">Next &rsaquo;</span></c:otherwise>
        </c:choose>
    </nav>
</c:if>
