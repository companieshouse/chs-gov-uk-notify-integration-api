package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.MessageType.CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE;

public class ConfirmYouAreAnOwnerMemberEmailData extends ConfirmYouAreAMemberEmailData {

    public ConfirmYouAreAnOwnerMemberEmailData() {
    }

    public ConfirmYouAreAnOwnerMemberEmailData(final String to, final String addedBy, final String acspName, final String signinUrl) {
        super(to, addedBy, acspName, signinUrl);
    }

    @Override
    public String toNotificationSentLoggingMessage() {
        return String.format("%s notification sent. %s was added to %s by %s.", CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getAddedBy());
    }

    @Override
    public String toNotificationSendingFailureLoggingMessage() {
        return String.format("Failed to send %s notification. Details: to=%s, acspName=%s, addedBy=%s.", CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue(), getTo(), getAcspName(), getAddedBy());
    }

}
