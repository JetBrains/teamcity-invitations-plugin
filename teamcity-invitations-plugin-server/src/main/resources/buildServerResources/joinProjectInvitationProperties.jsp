<%@ page import="jetbrains.buildServer.serverSide.auth.Permission" %>
<%@ include file="/include-internal.jsp" %>
<%--
  ~ Copyright 2000-2020 JetBrains s.r.o.
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

<%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>
<%@ taglib prefix="roles" tagdir="/WEB-INF/tags/roles" %>

<table class="runnerFormTable" style="width: 99%;">
    <tr class="greyNote">
        <td colspan="2">
            <span class="greyNote">Invite users to join the <c:out value="${project.name}"/></span>
        </td>
    </tr>

    <tr>
        <td><label for="role">Role:</label></td>
        <td>
            <forms:select id="role" name="role" enableFilter="true" className="longField">
                <forms:option value="">&lt;Don't assign any role&gt;</forms:option>
                <c:forEach items="${roles}" var="role">
                    <%--@elvariable id="role" type="jetbrains.buildServer.serverSide.auth.Role"--%>
                    <forms:option value="${role.id}" title="${role.name}" selected="${role.id eq roleId}">
                        <c:out value="${role.name}"/>
                    </forms:option>
                </c:forEach>
            </forms:select>

            <span class="smallNote">
                Select a role that will be given to the invited user (<jsp:include page="fragments/rolesPopup.jsp">
                <jsp:param name="roles" value="${roles}"/>
            </jsp:include>)
            </span>
            <span id="error_role" class="roleOrGroupError error" style="display: none;"></span>
        </td>
    </tr>

    <tr>
        <td><label for="group">Group:</label></td>
        <td>
            <forms:select id="group" name="group" enableFilter="true" className="longField">
                <forms:option value="">&lt;Don't add to any group&gt;</forms:option>
                <c:forEach items="${groups}" var="group">
                    <%--@elvariable id="group" type="jetbrains.buildServer.groups.SUserGroup"--%>
                    <forms:option value="${group.key}" title="${group.name}" selected="${group.key eq groupKey}">
                        <c:out value="${group.name}"/>
                    </forms:option>
                </c:forEach>
            </forms:select>
            <span class="smallNote">
                Select a group where invited user will be added. <br/>
                Only groups with '<%=Permission.VIEW_PROJECT.getName()%>' permission are listed.
            </span>

            <span id="error_group" class="roleOrGroupError error" style="display: none;"></span>
        </td>
    </tr>

    <%@ include file="fragments/displayNameParam.jspf" %>
    <%@ include file="fragments/welcomeTextParam.jspf" %>
    <%@ include file="fragments/reusableParam.jspf" %>

</table>

<script type="application/javascript">
    var changeSelectors = function () {
        if ($j("#group").get(0).selectedIndex != 0 || $j("#role").get(0).selectedIndex != 0) {
            $j(".modalDialog").find(".submitButton").prop("disabled", false);
            $j('.roleOrGroupError').text("");
            $j('.roleOrGroupError').hide();
        } else {
            $j(".modalDialog").find(".submitButton").prop("disabled", true);
            $j('.roleOrGroupError').show();
            $j('.roleOrGroupError').text("Please select a role or a group");
        }
    };

    $("group").on('change', changeSelectors);
    $("role").on('change', changeSelectors);
    changeSelectors();
</script>
