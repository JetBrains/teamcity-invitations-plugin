<%@ include file="/include-internal.jsp" %>

<input type="hidden" name="invitationType" value="existingProjectInvitation"/>
<div>
    <span>
        Invite user to join the existing project.
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
        <label for="project">Project: <l:star/></label>
        <forms:select id="project" name="project" enableFilter="true" className="textField">
            <c:forEach items="${projects}" var="project">
                <%--@elvariable id="project" type="jetbrains.buildServer.serverSide.SProject"--%>
                <forms:option value="${project.externalId}" title="${project.name}"
                              selected="${project.externalId eq project}">
                    <c:out value="${project.name}"/>
                </forms:option>
            </c:forEach>
        </forms:select>
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