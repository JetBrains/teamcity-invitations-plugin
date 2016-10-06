package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import jetbrains.buildServer.util.StringUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

@ThreadSafe
public class InvitationsStorage {

    private final TeamCityCoreFacade teamCityCore;
    private Map<String, Invitation> invitations;

    public InvitationsStorage(@NotNull TeamCityCoreFacade teamCityCore) {
        this.teamCityCore = teamCityCore;
    }

    public synchronized String createUserAndProjectInvitation(@NotNull String registrationUrl, String parentProjectExtId) {
        loadFromFile();
        String token = StringUtil.generateUniqueHash();
        CreateUserAndProjectInvitation invitation = new CreateUserAndProjectInvitation(token, registrationUrl,
                parentProjectExtId, "PROJECT_ADMIN");
        invitation.setTeamCityCore(teamCityCore);
        invitations.put(token, invitation);
        persist();
        Loggers.SERVER.info("User invitation with token " + token + " created: " + invitation.getDescription());
        return token;
    }

    @Nullable
    public synchronized Invitation getInvitation(@NotNull String token) {
        loadFromFile();
        return invitations.get(token);
    }

    @NotNull
    public synchronized Map<String, InvitationDescription> getInvitations() {
        loadFromFile();
        return new HashMap<>(invitations);
    }

    private synchronized void loadFromFile() {
        if (invitations != null) return;

        invitations = new HashMap<>();
        File invitationsFile = getInvitationsFile("inviteProjectAdmin");
        try {
            Element rootEl = FileUtil.parseDocument(invitationsFile);
            for (Object invitationEl : rootEl.getChildren()) {
                CreateUserAndProjectInvitation invitation = CreateUserAndProjectInvitation.from((Element) invitationEl);
                invitation.setTeamCityCore(teamCityCore);
                invitations.put(invitation.getToken(), invitation);
            }
        } catch (Exception e) {
            Loggers.SERVER.warnAndDebugDetails("Failed to load invitations from file: " + invitationsFile.getAbsolutePath(), e);
        }
    }

    private synchronized void persist() {
        Document doc = new Document();
        Element rootElem = new Element("invitations");
        doc.addContent(rootElem);
        invitations.forEach((token, invitation) -> {
            Element invitationEl = new Element("invitation");
            invitation.writeTo(invitationEl);
            rootElem.addContent(invitationEl);
        });

        File file = getInvitationsFile("inviteProjectAdmin");
        try {
            FileUtil.saveDocument(doc, file);
        } catch (IOException e) {
            Loggers.SERVER.warnAndDebugDetails("Failed to save invitations to file: " + file.getAbsolutePath(), e);
        }
    }

    @NotNull
    private File getInvitationsFile(String invitationType) {
        File invitationsDir = new File(teamCityCore.getPluginDataDir(), "invitations");
        File file = new File(invitationsDir, invitationType + ".xml");
        FileUtil.createIfDoesntExist(file);
        return file;
    }
}
