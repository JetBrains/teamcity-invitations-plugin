BS.InvitationsDialog = OO.extend(BS.AbstractWebForm, OO.extend(BS.AbstractModalDialog, {
    getContainer: function () {
        return $('invitationsFormDialog');
    },

    formElement: function () {
        return $('invitationsForm');
    },

    submit: function () {
        var that = this;
        BS.FormSaver.save(this, this.formElement().action, OO.extend(BS.SimpleListener, {
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
    }
}));