<%@ tag body-content="empty" pageEncoding="UTF-8" trimDirectiveWhitespaces="true" description="Star rating pill" %>
<%@ attribute name="value" required="true" type="java.math.BigDecimal" %>
<%@ taglib prefix="fmt" uri="jakarta.tags.fmt" %>
<span class="rating ${value >= 4 ? 'rating-good' : (value >= 3 ? 'rating-ok' : 'rating-low')}" title="Rated ${value} out of 5"><fmt:formatNumber value="${value}" minFractionDigits="1" maxFractionDigits="1"/> &#9733;</span>
