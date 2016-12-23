<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.JoinProjectInvitationType.InvitationImpl"--%>

<div>
    Role:
    <c:choose>
        <c:when test="${invitation.role != null}">
            <a target="_blank" href="/admin/admin.html?item=roles#${invitation.role.id}">${invitation.role.name}</a>
        </c:when>
        <c:otherwise>
            not specified
        </c:otherwise>
    </c:choose>
    <br/>
    Group:
    <c:choose>
        <c:when test="${invitation.group != null}">
            <a target="_blank"
               href="/admin/editGroup.html?groupCode=${invitation.group.key}">${invitation.group.name}</a>
        </c:when>
        <c:otherwise>
            not specified
        </c:otherwise>
    </c:choose>
    <br/>
    Reusable: <c:if test="${invitation.reusable}">Yes</c:if><c:if test="${!invitation.reusable}">No</c:if>
</div>