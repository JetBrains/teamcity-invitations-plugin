<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ include file="/include-internal.jsp" %>
<%--
  ~ Copyright 2000-2021 JetBrains s.r.o.
  ~
  ~ Licensed under the Apache License, Version 2.0 (the "License");
  ~ you may not use this file except in compliance with the License.
  ~ You may obtain a copy of the License at
  ~
  ~ http://www.apache.org/licenses/LICENSE-2.0
  ~
  ~ Unless required by applicable law or agreed to in writing, software
  ~ distributed under the License is distributed on an "AS IS" BASIS,
  ~ WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
  ~ See the License for the specific language governing permissions and
  ~ limitations under the License.
  --%>

<%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.CreateNewProjectInvitationType.InvitationImpl"--%>

<div>
    Role:
    <c:choose>
        <c:when test="${invitation.role != null}">
            <c:out value="${invitation.role.name}"/>
        </c:when>
        <c:otherwise>
            <c:out value="${invitation.roleId}"/>
        </c:otherwise>
    </c:choose>
    <br/>
    Reusable: <c:if test="${invitation.reusable}">Yes</c:if><c:if test="${!invitation.reusable}">No</c:if>
    <br/>
    Welcome text: <bs:trimWithTooltip maxlength="25"><c:out value="${invitation.welcomeText}"/></bs:trimWithTooltip>
</div>