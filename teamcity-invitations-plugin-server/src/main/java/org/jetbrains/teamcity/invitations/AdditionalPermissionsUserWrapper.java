package org.jetbrains.teamcity.invitations;

import jetbrains.buildServer.groups.UserGroup;
import jetbrains.buildServer.notification.DuplicateNotificationRuleException;
import jetbrains.buildServer.notification.NotificationRule;
import jetbrains.buildServer.notification.NotificationRulesHolder;
import jetbrains.buildServer.notification.WatchedBuilds;
import jetbrains.buildServer.serverSide.BuildTypeFilter;
import jetbrains.buildServer.serverSide.SBuildType;
import jetbrains.buildServer.serverSide.SProject;
import jetbrains.buildServer.serverSide.auth.*;
import jetbrains.buildServer.users.*;
import jetbrains.buildServer.users.impl.UserEx;
import jetbrains.buildServer.vcs.InvalidVcsNameException;
import jetbrains.buildServer.vcs.SVcsModification;
import jetbrains.buildServer.vcs.VcsRoot;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.util.*;
import java.util.concurrent.atomic.AtomicBoolean;

public class AdditionalPermissionsUserWrapper implements UserEx {
    private final UserEx delegate;
    private final Map<String, List<Permission>> additionalPermissions;
    private final AtomicBoolean enabled = new AtomicBoolean(true);

    public AdditionalPermissionsUserWrapper(@NotNull UserEx originalUser,
                                            @NotNull Map<String, List<Permission>> additionalPermissions) {
        this.delegate = originalUser;
        this.additionalPermissions = Collections.unmodifiableMap(new HashMap<>(additionalPermissions));
    }

    public void disable() {
        enabled.set(false);
    }

    @Override
    public boolean isPermissionGrantedForProject(@NotNull String projectId, @NotNull Permission permission) {
        if (enabled.get()) {
            List<Permission> projectPermissions = additionalPermissions.get(projectId);
            if (projectPermissions != null && projectPermissions.contains(permission)) {
                return true;
            }
        }

        return delegate.isPermissionGrantedForProject(projectId, permission);
    }

    @Override
    public boolean isPermissionGrantedForAnyProject(@NotNull Permission permission) {
        if (!enabled.get()) return delegate.isPermissionGrantedForAnyProject(permission);

        return delegate.isPermissionGrantedForAnyProject(permission) || additionalPermissions.values().stream().anyMatch(list -> list.contains(permission));
    }

    @NotNull
    @Override
    public Permissions getPermissionsGrantedForProject(@NotNull String projectId) {
        if (enabled.get()) {
            List<Permission> projectPermissions = additionalPermissions.get(projectId);
            if (projectPermissions != null) {
                List<Permission> base = delegate.getPermissionsGrantedForProject(projectId).toList();
                base.addAll(projectPermissions);
                return new Permissions(base);
            }
        }

        return delegate.getPermissionsGrantedForProject(projectId);
    }

    @Override
    public void updatePermissions() {
        delegate.updatePermissions();
    }

    @Override
    public boolean isHasHiddenProjects() {
        return delegate.isHasHiddenProjects();
    }

    @Override
    public boolean isConfiguredVisibleProjects() {
        return delegate.isConfiguredVisibleProjects();
    }

    @Override
    @NotNull
    public SortedMap<SProject, List<SBuildType>> getFilteredVisibleBuildTypes(@Nullable BuildTypeFilter buildTypeFilter) {
        return delegate.getFilteredVisibleBuildTypes(buildTypeFilter);
    }

    @Override
    public void resetBuildTypesOrder(@NotNull SProject sProject) {
        delegate.resetBuildTypesOrder(sProject);
    }

    @Override
    public boolean hasPassword() {
        return delegate.hasPassword();
    }

    @Override
    public boolean isPseudoUser() {
        return delegate.isPseudoUser();
    }

    @Override
    public boolean setEmailIsVerified(@NotNull String s) {
        return delegate.setEmailIsVerified(s);
    }

    @Override
    @Nullable
    public String getVerifiedEmail() {
        return delegate.getVerifiedEmail();
    }

    @Override
    @NotNull
    public Map<VcsUsernamePropertyKey, List<String>> getVcsUsernames() {
        return delegate.getVcsUsernames();
    }

    @Override
    public void setDefaultVcsUsernames(@NotNull List<String> list) throws InvalidVcsNameException {
        delegate.setDefaultVcsUsernames(list);
    }

    @Override
    public void setVcsUsernames(@NotNull String s, @NotNull List<String> list) throws InvalidVcsNameException {
        delegate.setVcsUsernames(s, list);
    }

    @Override
    public void setVcsRootUsernames(@NotNull VcsRoot vcsRoot, @NotNull List<String> list) throws InvalidVcsNameException {
        delegate.setVcsRootUsernames(vcsRoot, list);
    }

    @Override
    public void setVcsUsernames(@NotNull VcsUsernamePropertyKey vcsUsernamePropertyKey, @NotNull List<String> list) throws InvalidVcsNameException {
        delegate.setVcsUsernames(vcsUsernamePropertyKey, list);
    }

    @Override
    public void addVcsUsername(@NotNull VcsUsernamePropertyKey vcsUsernamePropertyKey, @NotNull String s) throws InvalidVcsNameException {
        delegate.addVcsUsername(vcsUsernamePropertyKey, s);
    }

    @Override
    @NotNull
    public List<SVcsModification> getVcsModifications(int numberOfActiveDays) {
        return delegate.getVcsModifications(numberOfActiveDays);
    }

    @Override
    @NotNull
    public List<SVcsModification> getAllModifications() {
        return delegate.getAllModifications();
    }

    @Override
    public void updateUserAccount(@NotNull String username, String name, String email) throws UserNotFoundException, DuplicateUserAccountException, EmptyUsernameException {
        delegate.updateUserAccount(username, name, email);
    }

    @Override
    public void setUserProperties(@NotNull Map<? extends PropertyKey, String> properties) throws UserNotFoundException {
        delegate.setUserProperties(properties);
    }

    @Override
    public void setUserProperty(@NotNull PropertyKey propertyKey, String value) throws UserNotFoundException {
        delegate.setUserProperty(propertyKey, value);
    }

    @Override
    public void deleteUserProperty(@NotNull PropertyKey propertyKey) throws UserNotFoundException {
        delegate.deleteUserProperty(propertyKey);
    }

    @Override
    public void setPassword(String password) throws UserNotFoundException {
        delegate.setPassword(password);
    }

    @Override
    @NotNull
    public List<String> getProjectsOrder() throws UserNotFoundException {
        return delegate.getProjectsOrder();
    }

    @Override
    public void setProjectsOrder(@NotNull List<String> projectsOrder) throws UserNotFoundException {
        delegate.setProjectsOrder(projectsOrder);
    }

    @Override
    public void hideProject(@NotNull String projectId) throws UserNotFoundException {
        delegate.hideProject(projectId);
    }

    @Override
    public void setBlockState(String blockType, String blockState) {
        delegate.setBlockState(blockType, blockState);
    }

    @Override
    @Nullable
    public String getBlockState(String blockType) {
        return delegate.getBlockState(blockType);
    }

    @Override
    @NotNull
    public List<UserGroup> getUserGroups() {
        return delegate.getUserGroups();
    }

    @Override
    @NotNull
    public List<UserGroup> getAllUserGroups() {
        return delegate.getAllUserGroups();
    }

    @Override
    @NotNull
    public List<VcsUsernamePropertyKey> getVcsUsernameProperties() {
        return delegate.getVcsUsernameProperties();
    }

    @Override
    @NotNull
    public List<SBuildType> getOrderedBuildTypes(@Nullable SProject project) {
        return delegate.getOrderedBuildTypes(project);
    }

    @Override
    @NotNull
    public Collection<SBuildType> getBuildTypesOrder(@NotNull SProject project) {
        return delegate.getBuildTypesOrder(project);
    }

    @Override
    public void setBuildTypesOrder(@NotNull SProject project, @NotNull List<SBuildType> visible, @NotNull List<SBuildType> invisible) {
        delegate.setBuildTypesOrder(project, visible, invisible);
    }

    @Override
    public boolean isHighlightRelatedDataInUI() {
        return delegate.isHighlightRelatedDataInUI();
    }

    @Override
    public boolean isPermissionGrantedGlobally(@NotNull Permission permission) {
        return delegate.isPermissionGrantedGlobally(permission);
    }

    @Override
    @NotNull
    public Permissions getGlobalPermissions() {
        return delegate.getGlobalPermissions();
    }

    @Override
    @NotNull
    public Map<String, Permissions> getProjectsPermissions() {
        return delegate.getProjectsPermissions();
    }

    @Override
    public boolean isPermissionGrantedForAllProjects(@NotNull Collection<String> projectIds, @NotNull Permission permission) {
        return delegate.isPermissionGrantedForAllProjects(projectIds, permission);
    }

    @Override
    @NotNull
    public Permissions getPermissionsGrantedForAllProjects(@NotNull Collection<String> projectIds) {
        return delegate.getPermissionsGrantedForAllProjects(projectIds);
    }

    @Override
    @Nullable
    public User getAssociatedUser() {
        return delegate.getAssociatedUser();
    }

    @Override
    @NotNull
    public Collection<Role> getRolesWithScope(@NotNull RoleScope scope) {
        return delegate.getRolesWithScope(scope);
    }

    @Override
    public Collection<RoleScope> getScopes() {
        return delegate.getScopes();
    }

    @Override
    @NotNull
    public Collection<RoleEntry> getRoles() {
        return delegate.getRoles();
    }

    @Override
    public void addRole(@NotNull RoleScope scope, @NotNull Role role) {
        delegate.addRole(scope, role);
    }

    @Override
    public void removeRole(@NotNull RoleScope scope, @NotNull Role role) {
        delegate.removeRole(scope, role);
    }

    @Override
    public void removeRole(@NotNull Role role) {
        delegate.removeRole(role);
    }

    @Override
    public void removeRoles(@NotNull RoleScope scope) {
        delegate.removeRoles(scope);
    }

    @Override
    public boolean isSystemAdministratorRoleGranted() {
        return delegate.isSystemAdministratorRoleGranted();
    }

    @Override
    public boolean isSystemAdministratorRoleGrantedDirectly() {
        return delegate.isSystemAdministratorRoleGrantedDirectly();
    }

    @Override
    public boolean isSystemAdministratorRoleInherited() {
        return delegate.isSystemAdministratorRoleInherited();
    }

    @Override
    @NotNull
    public Collection<RolesHolder> getParentHolders() {
        return delegate.getParentHolders();
    }

    @Override
    @NotNull
    public Collection<RolesHolder> getAllParentHolders() {
        return delegate.getAllParentHolders();
    }

    @Override
    @NotNull
    public String describe(boolean verbose) {
        return delegate.describe(verbose);
    }

    @Override
    public long getId() {
        return delegate.getId();
    }

    @Override
    public String getRealm() {
        return delegate.getRealm();
    }

    @Override
    public String getUsername() {
        return delegate.getUsername();
    }

    @Override
    public String getName() {
        return delegate.getName();
    }

    @Override
    public String getEmail() {
        return delegate.getEmail();
    }

    @Override
    public String getDescriptiveName() {
        return delegate.getDescriptiveName();
    }

    @Override
    public String getExtendedName() {
        return delegate.getExtendedName();
    }

    @Override
    public Date getLastLoginTimestamp() {
        return delegate.getLastLoginTimestamp();
    }

    @Override
    public void setLastLoginTimestamp(@NotNull Date timestamp) throws UserNotFoundException {
        delegate.setLastLoginTimestamp(timestamp);
    }

    @Override
    public List<String> getVisibleProjects() {
        return delegate.getVisibleProjects();
    }

    @Override
    public void setVisibleProjects(@NotNull Collection<String> visibleProjects) throws UserNotFoundException {
        delegate.setVisibleProjects(visibleProjects);
    }

    @Override
    public List<String> getAllProjects() {
        return delegate.getAllProjects();
    }

    @Override
    @Nullable
    public String getPropertyValue(PropertyKey propertyKey) {
        return delegate.getPropertyValue(propertyKey);
    }

    @Override
    public boolean getBooleanProperty(PropertyKey propertyKey) {
        return delegate.getBooleanProperty(propertyKey);
    }

    @Override
    @NotNull
    public Map<PropertyKey, String> getProperties() {
        return delegate.getProperties();
    }

    @Override
    public void setProperties(@NotNull Map<? extends PropertyKey, String> map) {
        delegate.setProperties(map);
    }

    @Override
    @NotNull
    public List<NotificationRule> getNotificationRules(@NotNull String notifierType) {
        return delegate.getNotificationRules(notifierType);
    }

    @Override
    public void setNotificationRules(@NotNull String notifierType, @NotNull List<NotificationRule> rules) {
        delegate.setNotificationRules(notifierType, rules);
    }

    @Override
    public void removeRule(long ruleId) {
        delegate.removeRule(ruleId);
    }

    @Override
    public void applyOrder(@NotNull String notifierType, @NotNull long[] ruleIds) {
        delegate.applyOrder(notifierType, ruleIds);
    }

    @Override
    public long addNewRule(@NotNull String notifierType, @NotNull NotificationRule rule) throws DuplicateNotificationRuleException {
        return delegate.addNewRule(notifierType, rule);
    }

    @Override
    @Nullable
    public Collection<Long> findConflictingRules(@NotNull String notifierType, @NotNull WatchedBuilds watch) {
        return delegate.findConflictingRules(notifierType, watch);
    }

    @Override
    @Nullable
    public NotificationRule findRuleById(long ruleId) {
        return delegate.findRuleById(ruleId);
    }

    @Override
    @NotNull
    public List<NotificationRulesHolder> getParentRulesHolders() {
        return delegate.getParentRulesHolders();
    }

    @Override
    @NotNull
    public List<NotificationRulesHolder> getAllParentRulesHolders() {
        return delegate.getAllParentRulesHolders();
    }

    @Override
    public void resetRoles() {
        delegate.resetRoles();
    }

    @Override
    public boolean setProperty(@NotNull PropertyKey propertyKey, String s) {
        return delegate.setProperty(propertyKey, s);
    }

    @Override
    public boolean deleteProperty(@NotNull PropertyKey propertyKey) {
        return delegate.deleteProperty(propertyKey);
    }

    @Override
    public int hashCode() {
        return delegate.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return delegate.equals(obj);
    }

    @Override
    public String toString() {
        return delegate.toString();
    }
}
