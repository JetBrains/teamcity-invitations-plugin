BS.InvitationsDialog = OO.extend(BS.AbstractWebForm, OO.extend(BS.AbstractModalDialog, {

    getContainer: function () {
        return $('invitationsFormDialog');
    },

    formElement: function () {
        return $('invitationsForm');
    },

    submit: function () {
        var that = this;
        BS.FormSaver.save(this, this.action, OO.extend(BS.SimpleListener, {
            onCompleteSave: function () {
                $('invitationsList').refresh();
                that.enable();
                BS.InvitationsDialog.close();
            }
        }));
        return false;
    },

    savingIndicator: function () {
        return $('invitationsFormProgress');
    },

    openNew: function () {
        this.init(null, "Add", "/admin/invitations.html?createInvitation=1", true, "/registerUser.html", "/admin/editProject.html?init=1&projectId={projectExtId}", "_Root");
        this.showCentered();
        return false;
    },

    openEdit: function (token, multiuser, registrationUrl, afterRegistrationUrl, parentExtId) {
        this.init(token, "Edit", "/admin/invitations.html?editInvitation=1", multiuser, registrationUrl, afterRegistrationUrl, parentExtId);
        this.showCentered();
        return false;
    },

    init: function (token, buttonText, action, multiuser, registrationUrl, afterRegistrationUrl, parentExtId) {
        this.action = action;
        $j('#token').prop("value", token);
        $j('#createInvitationSumbit').prop("value", buttonText);
        $j('#invitationsFormTitle').text(buttonText + " Invitation");
        $j('#multiuser').prop("checked", multiuser);
        $j('#registrationUrl').val(registrationUrl);
        $j('#afterRegistrationUrl').val(afterRegistrationUrl);
        $j('#parentProject > option').each(function () {
            if (this.value === parentExtId) {
                $('parentProject').setSelectValue(this.value);
            }
        });
    }
}));

BS.Invitations = {
    deleteInvitation: function (token) {
        BS.ajaxRequest('/admin/invitations.html?removeInvitation=' + token, {
            onComplete: function () {
                BS.reload();
            }
        });
    }
};