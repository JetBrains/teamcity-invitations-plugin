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
import java.util.stream.Collectors;

import static java.util.function.Function.identity;

@ThreadSafe
public class InvitationsStorage {

    private final TeamCityCoreFacade teamCityCore;
    private final Map<String, InvitationType> invitationTypes;
    private Map<String, Invitation> invitations;

    public InvitationsStorage(@NotNull TeamCityCoreFacade teamCityCore,
                              @NotNull List<InvitationType> invitationTypes) {
        this.teamCityCore = teamCityCore;
        this.invitationTypes = invitationTypes.stream().collect(Collectors.toMap(InvitationType::getId, identity()));
    }

    public synchronized Invitation addInvitation(@NotNull String token, @NotNull Invitation invitation) {
        loadFromFile();
        invitations.put(token, invitation);
        persist();
        Loggers.SERVER.info("User invitation with token " + token + " created.");
        return invitation;
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

    public synchronized Invitation removeInvitation(@NotNull String token) {
        Invitation removed = invitations.remove(token);
        if (removed != null) persist();
        return removed;
    }

    private synchronized void loadFromFile() {
        if (invitations != null) return;

        invitations = new HashMap<>();
        File invitationsFile = getInvitationsFile();
        if (!invitationsFile.exists() || invitationsFile.length() == 0) return;

        try {
            Element rootEl = FileUtil.parseDocument(invitationsFile);
            for (Object invitationEl : rootEl.getChildren()) {
                try {
                    InvitationType invitationType = invitationTypes.get(((Element) invitationEl).getAttributeValue("type"));
                    Invitation invitation = invitationType.readFrom((Element) invitationEl);
                    invitations.put(invitation.getToken(), invitation);
                } catch (InvitationException e) {
                    Loggers.SERVER.warnAndDebugDetails("Failed to load invitation from element: " + invitationEl, e);

                }
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
            invitationEl.setAttribute("type", invitation.getType().getId());
            invitation.writeTo(invitationEl);
            rootElem.addContent(invitationEl);
        });

        File file = getInvitationsFile();
        FileUtil.createIfDoesntExist(file);
        try {
            FileUtil.saveDocument(doc, file);
        } catch (IOException e) {
            Loggers.SERVER.warnAndDebugDetails("Failed to save invitations to file: " + file.getAbsolutePath(), e);
        }
    }

    @NotNull
    private File getInvitationsFile() {
        File invitationsDir = new File(teamCityCore.getPluginDataDir(), "invitations");
        return new File(invitationsDir, "invitations.xml");
    }
}
