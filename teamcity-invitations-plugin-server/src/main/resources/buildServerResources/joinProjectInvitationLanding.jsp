<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.JoinProjectInvitationType.InvitationImpl"--%>
<%@ include file="/include-internal.jsp" %>
<c:set var="title" value="Join project invitation"/>
<bs:externalPage>
    <jsp:attribute name="page_title">${title}</jsp:attribute>
    <jsp:attribute name="head_include">
    <bs:linkCSS>
      /css/forms.css
      /css/initialPages.css
    </bs:linkCSS>
    <bs:linkScript>
      /js/bs/bs.js
    </bs:linkScript>
    <bs:ua/>
  </jsp:attribute>
    <jsp:attribute name="body_include">

    <bs:_loginPageDecoration id="loginPage" title="${title}">
      <p id="formNote">
         <p>
                 ${invitation.user.descriptiveName} invites you to join the ${invitation.project.fullName} project.
         </p>
        <c:choose>
          <c:when test="${loggedInUser == null}">
            <p>
                Please <a href="<c:url value='${proceedUrl}'/>">login or register</a> to accept the invitation.
            </p>
          </c:when>
          <c:otherwise>
            <%--@elvariable id="loggedInUser" type="jetbrains.buildServer.users.SUser"--%>
            <p>
                You are logged in as '${loggedInUser.descriptiveName}'. <br/>
                Please <a href="<c:url value='${proceedUrl}'/>">proceed</a> as a currently logged-in user to accept the
                invitation.
            </p>

          </c:otherwise>
        </c:choose>
      </p>
    </bs:_loginPageDecoration>
  </jsp:attribute>
</bs:externalPage>
