BS.CreateInvitationDialog = OO.extend(BS.AbstractWebForm, OO.extend(BS.AbstractModalDialog, {

    getContainer: function () {
        return $j('#createInvitationFormDialog').get(0);
    },

    formElement: function () {
        return $j('#createInvitationForm').get(0);
    },

    submit: function () {
        var that = this;
        BS.FormSaver.save(this, window['base_uri'] + "/admin/invitations.html?createInvitation=1", OO.extend(BS.SimpleListener, {
            onCompleteSave: function () {
                $('invitationsList').refresh();
                that.enable();
                BS.CreateInvitationDialog.close();
            }
        }));
        return false;
    },

    savingIndicator: function () {
        return $('addInvitationFormProgress');
    },

    open: function () {
        var chooser = $j("#invitationType").get(0);
        chooser.selectedIndex = 0;
        chooser.value = chooser.options[0].value;
        this.doReloadInvitationType(false, null, true);
        return false;
    },

    reloadInvitationType: function (projectId) {
        var chooser = $j("#invitationType").get(0);
        this.doReloadInvitationType(chooser.selectedIndex != 0, "invitationType=" + encodeURIComponent(chooser.value) + "&addInvitation=1&projectId=" + projectId, false);
    },

    doReloadInvitationType: function (typeIsSelected, params, show) {
        var that = this;
        var dialog = $j(that.getContainer());

        var submitButton = dialog.find(".submitButton");
        submitButton.prop("disabled", true);

        if (typeIsSelected) {
            that.showCenteredOrRecenter(show);
            var progress = submitButton.siblings(".progressRing").show();
            dialog.find(".content").load(window['base_uri'] + "/admin/invitations.html", params, function () {
                progress.hide();
                submitButton.prop("disabled", false);
                that.recenterDialog();
            });
        } else {
            that.showCenteredOrRecenter(show);
        }
    },

    showCenteredOrRecenter: function (show) {
        if (show) {
            this.showCentered();
        } else {
            this.recenterDialog();
        }
    }
}));

BS.EditInvitationDialog = OO.extend(BS.AbstractWebForm, OO.extend(BS.AbstractModalDialog, {

    getContainer: function () {
        return $j('#editInvitationFormDialog').get(0);
    },

    formElement: function () {
        return $j('#editInvitationForm').get(0);
    },

    submit: function () {
        var that = this;
        BS.FormSaver.save(this, window['base_uri'] + "/admin/invitations.html?editInvitation=1", OO.extend(BS.SimpleListener, {
            onCompleteSave: function () {
                $('invitationsList').refresh();
                that.enable();
                BS.EditInvitationDialog.close();
            }
        }));
        return false;
    },

    savingIndicator: function () {
        return $('editInvitationsFormProgress');
    },

    open: function (token, projectId) {
        var that = this;
        var dialog = $j(that.getContainer());
        var submitButton = dialog.find(".submitButton");
        submitButton.prop("disabled", true);

        that.showCentered();
        var progress = submitButton.siblings(".progressRing").show();
        dialog.find(".content").load(window['base_uri'] + "/admin/invitations.html", "token=" + encodeURIComponent(token) + "&editInvitation=1&projectId=" + projectId, function () {
            progress.hide();
            submitButton.prop("disabled", false);
            that.recenterDialog();
        });

    }
}));

BS.Invitations = {
    deleteInvitation: function (token) {
        BS.ajaxRequest(window['base_uri'] + '/admin/invitations.html?removeInvitation=' + token, {
            onComplete: function () {
                BS.reload();
            }
        });
    }
};