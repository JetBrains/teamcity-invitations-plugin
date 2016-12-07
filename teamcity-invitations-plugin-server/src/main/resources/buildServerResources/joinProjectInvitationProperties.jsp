<%@ include file="/include-internal.jsp" %>
<%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>
<input type="hidden" name="projectId" value="${project.externalId}"/>
<input type="hidden" name="invitationType" value="existingProjectInvitation"/>
<div>
    <span>
        Invite user to join the <c:out value="${project.fullName}"/> project.
    </span>

    <input type="hidden" name="token" id="token" value="${token}"/>

    <div class="spacing"></div>
    <div><label for="name">Invitation name: <l:star/></label></div>
    <div><forms:textField name="name" value="${name}"/></div>

    <div class="spacing"></div>
    <div>
        <forms:checkbox name="multiuser" checked="${multiuser}"/>
        <label for="multiuser">Allow invitation to be used multiple times.</label>
    </div>

    <div class="spacing"></div>

    <div>
        <label for="role">Give user a role in the selected project: <l:star/></label>
        <forms:select id="role" name="role" enableFilter="true" className="textField">
            <c:forEach items="${roles}" var="role">
                <%--@elvariable id="role" type="jetbrains.buildServer.serverSide.auth.Role"--%>
                <forms:option value="${role.id}" title="${role.name}" selected="${role.id eq roleId}">
                    <c:out value="${role.name}"/>
                </forms:option>
            </c:forEach>
        </forms:select>
    </div>

</div>