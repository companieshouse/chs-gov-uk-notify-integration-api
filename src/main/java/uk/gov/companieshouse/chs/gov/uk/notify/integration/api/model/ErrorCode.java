package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

public enum ErrorCode {
    ERROR_CODE_1001("1001"),
    ERROR_CODE_1002("1002");

    private final String code;

    ErrorCode(final String code) {
        this.code = code;
    }

    public String getCode() {
        return String.format("ERROR CODE: %s", this.code);
    }
}
