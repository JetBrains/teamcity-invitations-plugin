<%@ taglib prefix="c" uri="http://java.sun.com/jsp/jstl/core" %>
<%@ taglib prefix="bs" tagdir="/WEB-INF/tags" %>
<%@ taglib prefix="user" uri="/WEB-INF/functions/user" %>


<%--@elvariable id="invitation" type="org.jetbrains.teamcity.invitations.JoinProjectInvitationType.InvitationImpl"--%>
<%@ include file="/include-internal.jsp" %>
<c:url value="/ajax.html?logout=1" var="logoutUrl"/>
<c:url value='${proceedUrl}' var="proceedFullUrl"/>

<bs:externalPage>
    <jsp:attribute name="page_title">${title}</jsp:attribute>
    <jsp:attribute name="head_include">
    <bs:linkCSS>
      /css/forms.css
      /css/maintenance-initialPages-common.css
      /css/initialPages.css
    </bs:linkCSS>
    <style type="text/css">
        <jsp:include page="css/invitations.css"/>
    </style>
    <bs:linkScript>
      /js/bs/bs.js
    </bs:linkScript>
    <bs:ua/>
  </jsp:attribute>
    <jsp:attribute name="body_include">

    <bs:_loginPageDecoration id="loginPage" title="${title}">
      <p id="formNote">
        <c:choose>
             <c:when test="${invitation == null}">
                The invitation is no longer actual. Either the one-time invitation was already used or the invitation was suspended. Proceed to the <a
                     href="<c:url value='/overview.html'/>">server</a>.
            </c:when>
             <c:when test="${!invitation.enabled}">
                 <c:choose>
                     <c:when test="${invitation.disabledText != null}">
                         <bs:out value="${invitation.disabledText}"/>
                     </c:when>
                     <c:otherwise>
                         This invitation is currently disabled, try again later.
                     </c:otherwise>
                 </c:choose>
            </c:when>
            <c:when test="${invitation.validationError != null}">
                This invitation is invalid, try again later.
            </c:when>
            <c:otherwise>
                <p>
                   <c:if test="${fn:length(welcomeText) > 0}">
                       <bs:out value="${welcomeText}"/>
                   </c:if>
                </p>
                <c:choose>
                  <%--@elvariable id="loggedInUser" type="jetbrains.buildServer.users.impl.UserEx"--%>
                    <c:when test="${loggedInUser == null}">
                    <p>
                        <a class="invitations__button" href="<c:url value='${proceedUrl}'/>">Log in and proceed...</a>
                    </p>
                  </c:when>
                  <c:when test="${loggedInUser != null && user:isGuestUser(loggedInUser)}">
                    <p>
                        You are logged in as a guest and cannot accept invitations. <br/>
                        <button type="button" class="invitations__button" onclick="BS.Invitations.logoutGuest();">Re-log in and proceed...</button>
                    </p>
                  </c:when>
                  <c:otherwise>
                    <%--@elvariable id="loggedInUser" type="jetbrains.buildServer.users.SUser"--%>
                      <p>
                          You are logged in as '<c:out value="${loggedInUser.descriptiveName}"/>'. <br/>
                          <a class="invitations__button" href="<c:url value='${proceedUrl}'/>">Proceed</a>
                      </p>
                  </c:otherwise>
                </c:choose>
           </c:otherwise>
        </c:choose>

    </bs:_loginPageDecoration>
  </jsp:attribute>
</bs:externalPage>

<script type="application/javascript">
    BS.Invitations = {
        logoutGuest: function () {
            BS.ajaxRequest("${logoutUrl}", {
                onComplete: function () {
                    document.location.href = "<c:url value='${proceedFullUrl}'/>";
                }
            });
        }
    };
</script>