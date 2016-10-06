package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.BaseTestCase;
import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.impl.TeamCityInternalKeys;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import jetbrains.buildServer.web.util.SessionUser;
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

import static jetbrains.buildServer.serverSide.auth.RoleScope.projectScope;
import static org.assertj.core.api.BDDAssertions.then;

@Test
public class InvitationsTest extends BaseTestCase {

    private InvitationsStorage invitations;
    private InvitationsController invitationsController;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private MockHttpSession session;
    private FakeTeamCityCoreFacade core;

    @BeforeMethod
    public void setUp() throws Exception {
        super.setUp();
        core = new FakeTeamCityCoreFacade(createTempDir());
        core.addRole("PROJECT_ADMIN");
        invitations = new InvitationsStorage(core);
        invitationsController = new InvitationsController(Mockito.mock(WebControllerManager.class), invitations, Mockito.mock(AuthorizationInterceptor.class),
                Mockito.mock(RootUrlHolder.class));
    }

    @Test
    public void simple_invitation() throws Exception {
        String token = invitations.createUserAndProjectInvitation("/registerUser.html", "TestDriveProjectId", true);

        //user go to invitation url
        ModelAndView invitationResponse = goToInvitationUrl(token);
        assertRedirectTo(invitationResponse, "/registerUser.html");

        //user registered
        SUser user = core.createUser("oleg");
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl(user);
        then(afterRegistrationMAW.getView()).isInstanceOf(RedirectView.class);
        then(core.getProject("oleg project")).isNotNull();
        then(user.getRolesWithScope(projectScope("oleg project"))).extracting(Role::getId).contains("PROJECT_ADMIN");
    }

    public void should_survive_server_restart() {
        String token = invitations.createUserAndProjectInvitation("/registerUser.html", "TestDriveProjectId", true);

        invitations = new InvitationsStorage(core);

        then(invitations.getInvitation(token)).isNotNull();
    }

    public void remove_invitation() throws Exception {
        String token = invitations.createUserAndProjectInvitation("/registerUser.html", "TestDriveProjectId", true);
        invitations.removeInvitation(token);

        then(invitations.getInvitation(token)).isNull();

        //user go to invitation url
        ModelAndView invitationResponse = goToInvitationUrl(token);
        assertRedirectTo(invitationResponse, "/");
    }

    public void invitation_removed_during_user_registration() throws Exception {
        String token = invitations.createUserAndProjectInvitation("/registerUser.html", "TestDriveProjectId", true);

        //user go to invitation url
        goToInvitationUrl(token);

        invitations.removeInvitation(token);

        SUser user = core.createUser("oleg");
        ModelAndView afterRegistrationMAW = goToAfterRegistrationUrl(user);
        assertRedirectTo(afterRegistrationMAW, "/");
    }

    public void multiple_user_invitation_can_be_used_several_times() throws Exception {
        String token = invitations.createUserAndProjectInvitation("/registerUser.html", "TestDriveProjectId", true);

        //first
        assertRedirectTo(goToInvitationUrl(token), "/registerUser.html");
        ;
        goToAfterRegistrationUrl(core.createUser("oleg"));

        //second
        assertRedirectTo(goToInvitationUrl(token), "/registerUser.html");
        ;
        goToAfterRegistrationUrl(core.createUser("ivan"));

        then(core.getProject("oleg project")).isNotNull();
        then(core.getProject("ivan project")).isNotNull();
    }

    public void single_user_invitation_can_be_used_once() throws Exception {
        String token = invitations.createUserAndProjectInvitation("/registerUser.html", "TestDriveProjectId", false);

        //first
        assertRedirectTo(goToInvitationUrl(token), "/registerUser.html");
        ;
        goToAfterRegistrationUrl(core.createUser("oleg"));

        //second
        assertRedirectTo(goToInvitationUrl(token), "/");
        ;
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
}

