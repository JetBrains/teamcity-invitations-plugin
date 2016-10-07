package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.log.Loggers;
import jetbrains.buildServer.util.FileUtil;
import org.jdom.Document;
import org.jdom.Element;
import org.jetbrains.annotations.NotNull;

import javax.annotation.Nullable;
import javax.annotation.concurrent.ThreadSafe;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@ThreadSafe
public class InvitationsStorage {

    private final TeamCityCoreFacade teamCityCore;
    private Map<String, Invitation> invitations;

    public InvitationsStorage(@NotNull TeamCityCoreFacade teamCityCore) {
        this.teamCityCore = teamCityCore;
    }

    public synchronized String createUserAndProjectInvitation(@NotNull String token,
                                                              @NotNull String registrationUrl,
                                                              @NotNull String afterRegistrationUrl,
                                                              @NotNull String parentProjectExtId,
                                                              boolean multiuser) {
        loadFromFile();
        Invitation invitation = new Invitation(token, registrationUrl,
                afterRegistrationUrl, parentProjectExtId, "PROJECT_ADMIN", multiuser);
        invitation.setTeamCityCore(teamCityCore);
        invitations.put(token, invitation);
        persist();
        Loggers.SERVER.debug("User invitation with token " + token + " created.");
        return token;
    }

    @Nullable
    public synchronized Invitation getInvitation(@NotNull String token) {
        loadFromFile();
        return invitations.get(token);
    }

    @NotNull
    public synchronized List<Invitation> getInvitations() {
        loadFromFile();
        return new ArrayList<>(invitations.values());
    }

    public synchronized void removeInvitation(@NotNull String token) {
        Invitation removed = invitations.remove(token);
        if (removed != null) persist();
    }

    private synchronized void loadFromFile() {
        if (invitations != null) return;

        invitations = new HashMap<>();
        File invitationsFile = getInvitationsFile("inviteProjectAdmin");
        if (!invitationsFile.exists() || invitationsFile.length() == 0) return;

        try {
            Element rootEl = FileUtil.parseDocument(invitationsFile);
            for (Object invitationEl : rootEl.getChildren()) {
                Invitation invitation = Invitation.from((Element) invitationEl);
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
