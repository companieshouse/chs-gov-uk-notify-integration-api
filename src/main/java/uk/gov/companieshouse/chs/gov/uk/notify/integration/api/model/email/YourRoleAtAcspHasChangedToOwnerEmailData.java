package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.MessageType.YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE;

public class YourRoleAtAcspHasChangedToOwnerEmailData extends YourRoleAtAcspHasChangedEmailData {

    public YourRoleAtAcspHasChangedToOwnerEmailData() {
    }

    public YourRoleAtAcspHasChangedToOwnerEmailData(final String to, final String editedBy, final String acspName, final String signinUrl) {
        super(to, editedBy, acspName, signinUrl);
    }

    @Override
    public String toNotificationSentLoggingMessage() {
        return String.format("%s notification sent. %s role at %s was changed by %s.", YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getEditedBy());
    }

    @Override
    public String toNotificationSendingFailureLoggingMessage() {
        return String.format("Failed to send %s notification. Details: to=%s, acspName=%s, editedBy=%s.", YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getEditedBy());
    }

}
