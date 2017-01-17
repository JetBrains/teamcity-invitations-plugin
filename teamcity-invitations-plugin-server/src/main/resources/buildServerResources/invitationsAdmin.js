BS.InvitationDialog = OO.extend(BS.AbstractWebForm, OO.extend(BS.AbstractModalDialog, {

    getContainer: function () {
        return $('invitationFormDialog');
    },

    formElement: function () {
        return $('invitationForm');
    },

    savingIndicator: function () {
        return $('invitationFormProgress');
    },

    submit: function () {
        BS.FormSaver.save(this, window['base_uri'] + "/admin/invitations.html", OO.extend(BS.ErrorsAwareListener, {
            onCompleteSave: function (form, responseXML, err) {
                err = BS.XMLResponse.processErrors(responseXML, {}, BS.PluginPropertiesForm.propertiesErrorsHandler);

                form.setSaving(false);
                form.enable();
                if (!err) {
                    $('invitationsList').refresh();
                    form.enable();
                    BS.InvitationDialog.close();
                }
            }
        }));
        return false;
    },

    openAddDialog: function (projectId) {
        this.enable();
        $j('#invitationFormTitle').text('Create Invitation');
        $j('#invitationTypeContainer').show();
        $j('#readOnlyInvitationType').hide();

        $('invitationTypeSelect').setSelectValue('');
        this.formElement().token.value = '';
        this.formElement().invitationType.value = '';
        $j(BS.InvitationDialog.getContainer()).find(".content").html('');
        this.reloadInvitationType(projectId, true);
        return false;
    },

    openEditDialog: function (token, invitationTypeDescr, invitationTypeId, projectId) {
        this.enable();
        $j('#invitationFormTitle').text('Edit Invitation');
        $j('#invitationTypeContainer').hide();
        $j('#readOnlyInvitationType').show();
        $j('#readOnlyInvitationType').text(invitationTypeDescr);

        $('invitationTypeSelect').setSelectValue('');
        this.formElement().token.value = token;
        this.formElement().invitationType.value = invitationTypeId;
        $j(BS.InvitationDialog.getContainer()).find(".content").html('');
        this.reloadInvitationType(projectId, true);
        return false;
    },

    invitationTypeChanged: function (selector, projectId) {
        this.formElement().invitationType.value = '';
        $j(BS.InvitationDialog.getContainer()).find(".content").html('');
        if (selector.selectedIndex > 0) {
            this.formElement().invitationType.value = selector.options[selector.selectedIndex].value;
            this.reloadInvitationType(projectId, false);
        }
    },

    reloadInvitationType: function (projectId, show) {
        var chooser = $("invitationType");

        var that = this;
        var dialog = $j(that.getContainer());

        var submitButton = dialog.find(".submitButton");
        submitButton.prop("disabled", true);

        if (show) {
            that.showCentered();
        } else {
            that.recenterDialog();
        }

        if (this.formElement().token.value || this.formElement().invitationType.value) {
            var progress = submitButton.siblings(".progressRing").show();
            dialog.find(".content").load(window['base_uri'] + "/admin/invitations.html", "token=" + this.formElement().token.value + "&invitationType=" + encodeURIComponent(this.formElement().invitationType.value) + "&projectId=" + projectId, function () {
                progress.hide();
                submitButton.prop("disabled", false);
                that.recenterDialog();
            });
        }
    }
}));

BS.Invitations = {
    deleteInvitation: function (token, projectId) {
        BS.confirmDialog.show({
            text: "Are you sure you want to delete this invitation?",
            actionButtonText: "Delete",
            cancelButtonText: 'Cancel',
            title: "Delete invitation",
            action: function () {
                BS.ajaxRequest(window['base_uri'] + '/admin/invitations.html?removeInvitation=true&token=' + token + "&projectId=" + projectId, {
                    onComplete: function () {
                        $('invitationsList').refresh();
                    }
                });
            }
        });
    },

    setEnabled: function (token, projectId, enabled) {
        BS.ajaxRequest(window['base_uri'] + '/admin/invitations.html?setEnabled=' + enabled + '&token=' + token + "&projectId=" + projectId, {
            onComplete: function () {
                $('invitationsList').refresh();
            }
        });
    }
};