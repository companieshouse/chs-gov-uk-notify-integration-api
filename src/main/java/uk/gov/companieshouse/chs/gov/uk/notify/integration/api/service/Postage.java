package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

public enum Postage {

    FIRST_CLASS("first"), SECOND_CLASS("second"), ECONOMY("economy");

    private Postage(String type) {
        this.type = type;
    }

    private String type;

    @Override
    public String toString() {
        return type;
    }
}
