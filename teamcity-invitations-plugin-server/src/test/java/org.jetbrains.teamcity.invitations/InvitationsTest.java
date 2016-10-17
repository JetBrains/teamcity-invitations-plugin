package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.util.XmlUtil;
import jetbrains.buildServer.web.impl.TeamCityInternalKeys;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;
import org.mockito.Mockito;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static java.util.Arrays.asList;
import static jetbrains.buildServer.serverSide.auth.RoleScope.projectScope;
import static org.assertj.core.api.BDDAssertions.then;

@Test
public class InvitationsTest extends BaseTestCase {

    private InvitationsStorage invitations;
    private InvitationsController invitationsController;
    private CreateNewProjectInvitationType createNewProjectInvitationType;
    private JoinProjectInvitationType joinProjectInvitationType;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;
    private FakeTeamCityCoreFacade core;
    private SProject testDriveProject;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        core = new FakeTeamCityCoreFacade(createTempDir());
        core.addRole("PROJECT_ADMIN");
        core.addRole("PROJECT_DEVELOPER");
        testDriveProject = core.createProjectAsSystem("_Root", "TestDriveProjectId");
        createNewProjectInvitationType = new CreateNewProjectInvitationType(core);
        joinProjectInvitationType = new JoinProjectInvitationType(core);
        invitations = createInvitationStorage();
        invitationsController = new InvitationsController(Mockito.mock(WebControllerManager.class), invitations, Mockito.mock(AuthorizationInterceptor.class),
                Mockito.mock(RootUrlHolder.class));
    }

    @NotNull
    private InvitationsStorage createInvitationStorage() {
        return new InvitationsStorage(core, asList(createNewProjectInvitationType, joinProjectInvitationType));
    }

    @Test
    public void invite_user_to_create_a_project() throws Exception {
        String token = inviteToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", "{username} project", true).getToken();

        //user go to invitation url
        ModelAndView invitationResponse = goToInvitationUrl(token);
        assertRedirectTo(invitationResponse, "/registerUser.html");

        //user registered
        SUser user = core.createUser("oleg");
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl(user);
        then(afterRegistrationMAW.getView()).isInstanceOf(RedirectView.class);
        then(core.getProject("oleg project")).isNotNull();
        then(core.getProject("oleg project").getParentProjectExternalId()).isEqualTo("TestDriveProjectId");
        then(user.getRolesWithScope(projectScope("oleg"))).extracting(Role::getId).contains("PROJECT_ADMIN");
    }

    @Test
    public void invite_user_to_join_the_project() throws Exception {
        String token = inviteToJoinProject("PROJECT_DEVELOPER", "TestDriveProjectId", true).getToken();

        //user go to invitation url
        ModelAndView invitationResponse = goToInvitationUrl(token);
        assertRedirectTo(invitationResponse, "/registerUser.html");

        //user registered
        SUser user = core.createUser("oleg");
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl(user);
        then(afterRegistrationMAW.getView()).isInstanceOf(RedirectView.class);
        then(user.getRolesWithScope(projectScope("TestDriveProjectId"))).extracting(Role::getId).contains("PROJECT_DEVELOPER");
    }

    @Test
    public void project_with_such_name_already_exists() throws Exception {
        String token = inviteToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", "{username} project", true).getToken();
        core.createProjectAsSystem("TestDriveProjectId", "oleg");

        //user go to invitation url
        ModelAndView invitationResponse = goToInvitationUrl(token);
        assertRedirectTo(invitationResponse, "/registerUser.html");

        //user registered
        SUser user = core.createUser("oleg");
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl(user);
        then(afterRegistrationMAW.getView()).isInstanceOf(RedirectView.class);
        then(core.getProject("oleg project1")).isNotNull();
        then(user.getRolesWithScope(projectScope("oleg project1"))).extracting(Role::getId).contains("PROJECT_ADMIN");
    }

    public void should_survive_server_restart() {
        String token1 = inviteToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", "{username} project", true).getToken();
        String token2 = inviteToJoinProject("PROJECT_DEVELOPER", "_Root", false).getToken();
        Element beforeRestartEl1 = new Element("invitation");
        Element beforeRestartEl2 = new Element("invitation");
        invitations.getInvitation(token1).writeTo(beforeRestartEl1);
        invitations.getInvitation(token2).writeTo(beforeRestartEl2);

        invitations = createInvitationStorage();

        then(invitations.getInvitation(token1)).isNotNull();
        then(invitations.getInvitation(token2)).isNotNull();

        Element afterRestartEl1 = new Element("invitation");
        Element afterRestartEl2 = new Element("invitation");
        invitations.getInvitation(token1).writeTo(afterRestartEl1);
        invitations.getInvitation(token2).writeTo(afterRestartEl2);
        then(XmlUtil.to_s(afterRestartEl1)).isEqualTo(XmlUtil.to_s(beforeRestartEl1));
        then(XmlUtil.to_s(afterRestartEl2)).isEqualTo(XmlUtil.to_s(beforeRestartEl2));
    }

    public void remove_invitation() throws Exception {
        String token = inviteToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", "{username} project", true).getToken();
        invitations.removeInvitation(token);

        then(invitations.getInvitation(token)).isNull();

        //user go to invitation url
        ModelAndView invitationResponse = goToInvitationUrl(token);
        assertRedirectTo(invitationResponse, "/");
    }

    public void invitation_removed_during_user_registration() throws Exception {
        String token = inviteToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", "{username} project", true).getToken();

        //user go to invitation url
        goToInvitationUrl(token);

        invitations.removeInvitation(token);

        SUser user = core.createUser("oleg");
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl(user);
        assertRedirectTo(afterRegistrationMAW, "/");
    }

    public void multiple_user_invitation_can_be_used_several_times() throws Exception {
        String token = inviteToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", "{username} project", true).getToken();

        //first
        assertRedirectTo(goToInvitationUrl(token), "/registerUser.html");
        goToAfterRegistrationUrl(core.createUser("oleg"));

        //second
        assertRedirectTo(goToInvitationUrl(token), "/registerUser.html");
        goToAfterRegistrationUrl(core.createUser("ivan"));

        then(core.getProject("oleg project")).isNotNull();
        then(core.getProject("ivan project")).isNotNull();
    }

    public void single_user_invitation_can_be_used_once() throws Exception {
        String token = inviteToCreateProject("PROJECT_ADMIN", "TestDriveProjectId", "{username} project", false).getToken();

        //first
        assertRedirectTo(goToInvitationUrl(token), "/registerUser.html");
        goToAfterRegistrationUrl(core.createUser("oleg"));

        //second
        assertRedirectTo(goToInvitationUrl(token), "/");
    }


    private ModelAndView goToAfterRegistrationUrl(SUser user) throws Exception {
        newRequest(HttpMethod.GET, (String) request.getSession().getAttribute(TeamCityInternalKeys.FIRST_LOGIN_REDIRECT_URL));
        SessionUser.setUser(request, user);
        return invitationsController.doHandle(request, response);
    }

    private ModelAndView goToInvitationUrl(String token) throws Exception {
        newRequest(HttpMethod.GET, "/invitations.html?token=" + token);
        return invitationsController.doHandle(request, response);
    }

    private void assertRedirectTo(ModelAndView invitationResponse, String expectedRedirect) {
        then(invitationResponse.getView()).isInstanceOf(RedirectView.class);
        then(((RedirectView) invitationResponse.getView()).getUrl()).isEqualTo(expectedRedirect);
    }

    private void newRequest(HttpMethod method, String url) {
        if (session == null) session = new MockHttpSession();
        request = MockMvcRequestBuilders.request(method, url).session(session).buildRequest(new MockServletContext());
        response = new MockHttpServletResponse();
    }

    private Invitation inviteToCreateProject(String role, String parentExtId, String newProjectName, boolean multiuser) {
        newRequest(HttpMethod.POST, "/admin/invitations.html?createInvitation=1");
        request.addParameter("parentProject", parentExtId);
        request.addParameter("role", role);
        request.addParameter("multiuser", multiuser + "");
        request.addParameter("newProjectName", newProjectName);
        return invitations.addInvitation("token", createNewProjectInvitationType.createNewInvitation(request, "token"));
    }

    private Invitation inviteToJoinProject(String role, String projectExtId, boolean multiuser) {
        newRequest(HttpMethod.POST, "/admin/invitations.html?createInvitation=1");
        request.addParameter("project", projectExtId);
        request.addParameter("role", role);
        request.addParameter("multiuser", multiuser + "");
        return invitations.addInvitation("token", joinProjectInvitationType.createNewInvitation(request, "token"));
    }
}

