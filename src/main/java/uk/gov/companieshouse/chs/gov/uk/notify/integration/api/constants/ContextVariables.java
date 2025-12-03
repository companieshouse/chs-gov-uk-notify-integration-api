package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants;

public class ContextVariables {

    private ContextVariables() {

    }

    // Resource paths
    public static final String ROOT_RESOURCE_PATH_VARIABLE = "root";
    public static final String LETTER_RESOURCE_PATH_VARIABLE = "letter";
    public static final String COMMON_RESOURCE_PATH_VARIABLE = "common";

    // Control variables
    public static final String IS_WELSH = "is_welsh";

    // Address fields
    public static final String ADDRESS_LINE_1 = "address_line_1";
    public static final String ADDRESS_LINE_2 = "address_line_2";
    public static final String ADDRESS_LINE_3 = "address_line_3";
    public static final String ADDRESS_LINE_4 = "address_line_4";
    public static final String ADDRESS_LINE_5 = "address_line_5";
    public static final String ADDRESS_LINE_6 = "address_line_6";
    public static final String ADDRESS_LINE_7 = "address_line_7";

    // Other letter template fields
    public static final String TODAYS_DATE = "todays_date";
    public static final String PSC_APPOINTMENT_DATE = "psc_appointment_date";
    public static final String IDV_START_DATE = "idv_start_date";
    public static final String IDV_VERIFICATION_DUE_DATE = "idv_verification_due_date";
    public static final String EXTENSION_REQUEST_DATE = "extension_request_date";
    public static final String REFERENCE = "reference";
    public static final String COMPANY_NAME = "company_name";
    public static final String COMPANY_NUMBER = "company_number";
    public static final String PSC_FULL_NAME = "psc_full_name";
    public static final String PSC_NAME = "psc_name";
    public static final String DEADLINE_DATE = "deadline_date";
    public static final String EXTENSION_DATE = "extension_date";
    public static final String TRIGGERING_EVENT_DATE = "triggering_event_date";
    public static final String VERIFICATION_DUE_DATE = "verification_due_date";
    public static final String TODAY_PLUS_28_DAYS = "today_plus_28_days";
    public static final String IS_LLP = "is_llp";
    public static final String REPRESENTATION_DUE_DATE = "representation_due_date";

    // Used only when recreating a previously sent letter for viewing, and never used
    // as the name of an actual context variable.
    public static final String ORIGINAL_SENDING_DATE = "original_sending_date";
}
