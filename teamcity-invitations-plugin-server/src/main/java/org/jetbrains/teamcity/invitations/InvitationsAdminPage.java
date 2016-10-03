package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.RootUrlHolder;
import jetbrains.buildServer.controllers.admin.AdminPage;
import jetbrains.buildServer.web.openapi.PagePlaces;
import jetbrains.buildServer.web.openapi.PluginDescriptor;
import org.jetbrains.annotations.NotNull;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

public class InvitationsAdminPage extends AdminPage {

    private final Invitations invitations;
    private RootUrlHolder rootUrlHolder;

    public InvitationsAdminPage(@NotNull PagePlaces pagePlaces,
                                @NotNull Invitations invitations,
                                @NotNull RootUrlHolder rootUrlHolder,
                                @NotNull PluginDescriptor pluginDescriptor) {
        super(pagePlaces, "invitations", pluginDescriptor.getPluginResourcesPath("invitationsAdmin.jsp"), "Invitations");
        this.invitations = invitations;
        this.rootUrlHolder = rootUrlHolder;
        register();
    }

    @Override
    public void fillModel(@NotNull Map<String, Object> model, @NotNull HttpServletRequest request) {
        model.put("invitations", invitations.getInvitations());
        model.put("invitationRootUrl", rootUrlHolder.getRootUrl() + InvitationsController.INVITATIONS_PATH);
    }

    @NotNull
    @Override
    public String getGroup() {
        return AdminPage.USER_MANAGEMENT_GROUP;
    }
}
