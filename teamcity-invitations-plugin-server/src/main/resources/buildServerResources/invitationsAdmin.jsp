<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<jsp:useBean id="invitations" type="java.util.Map" scope="request"/>

<c:if test="${not empty invitations}">

    <h2>Pending invitations</h2>

    <bs:refreshable containerId="invitationsList" pageUrl="${pageUrl}">
        <table class="dark borderBottom">
            <tr>
                <th>URL</th>
                <th>Description</th>
                <th>Multi-user</th>
            </tr>
            <c:forEach items="${invitations}" var="invitation">
                <tr>
                    <td>
                        <c:out value="${invitationRootUrl}?token=${invitation.key}"/>
                    </td>
                    <td>
                        <c:out value="${invitation.value.description}"/>
                    </td>
                    <td>
                        <c:out value="${invitation.value.multiUser}"/>
                    </td>
                </tr>
            </c:forEach>
        </table>
    </bs:refreshable>
</c:if>