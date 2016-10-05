package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.AuthorizationInterceptor;
import jetbrains.buildServer.serverSide.auth.Role;
import jetbrains.buildServer.users.SUser;
import jetbrains.buildServer.web.openapi.WebControllerManager;
import org.springframework.http.HttpMethod;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.mock.web.MockServletContext;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.view.RedirectView;
import org.testng.annotations.BeforeMethod;
import org.testng.annotations.Test;

import static jetbrains.buildServer.serverSide.auth.RoleScope.projectScope;
import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

@Test
public class InvitationsTest {

    private Invitations invitations;
    private InvitationsController invitationsController;
    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private FakeTeamCityCoreFacade core;

    @BeforeMethod
    public void setUp() throws Exception {
        core = new FakeTeamCityCoreFacade();
        core.addRole("PROJECT_ADMIN");
        invitations = new Invitations(core);
        invitationsController = new InvitationsController(mock(WebControllerManager.class), invitations, mock(AuthorizationInterceptor.class),
                mock(RootUrlHolder.class));
    }

    @Test
    public void simple_invitation() throws Exception {
        String token = invitations.createUserAndProjectInvitation("/registerUser.html", "TestDriveProjectId");

        //user go to invitation url
        newRequest(HttpMethod.GET, "/invitations.html?token=" + token);
        ModelAndView invitationResponse = invitationsController.doHandle(request, response);
        assertRedirectTo(invitationResponse, "/registerUser.html");

        //user registered
        SUser user = core.createUser("oleg");
        newRequest(HttpMethod.GET, "/overview.html");
        ModelAndView afterRegistrationMAW = invitations.getInvitation(token).userRegistered(user, request, response);
        assertThat(afterRegistrationMAW.getView()).isInstanceOf(RedirectView.class);
        assertThat(core.getProject("oleg project")).isNotNull();
        assertThat(user.getRolesWithScope(projectScope("oleg project"))).extracting(Role::getId).contains("PROJECT_ADMIN");
    }

    private void assertRedirectTo(ModelAndView invitationResponse, String expectedRedirect) {
        assertThat(invitationResponse.getView()).isInstanceOf(RedirectView.class);
        assertThat(((RedirectView) invitationResponse.getView()).getUrl()).isEqualTo(expectedRedirect);
    }

    private void newRequest(HttpMethod method, String url) {
        request = MockMvcRequestBuilders.request(method, url).buildRequest(new MockServletContext());
        response = new MockHttpServletResponse();
    }
}

