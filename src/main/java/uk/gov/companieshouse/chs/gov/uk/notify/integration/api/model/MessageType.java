package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

public enum MessageType {

    CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE("confirm_you_are_a_standard_member"),

    CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE("confirm_you_are_an_admin_member"),

    CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE("confirm_you_are_an_owner_member"),

    YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE("your_role_at_acsp_has_changed_to_standard"),

    YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE("your_role_at_acsp_has_changed_to_admin"),

    YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE("your_role_at_acsp_has_changed_to_owner");

    private final String value;

    MessageType(final String messageType) {
        this.value = messageType;
    }

    public String getValue() {
        return value;
    }

}
