package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions;

public class NotFoundRuntimeException extends RuntimeException {

    private final String fieldLocation;

    public NotFoundRuntimeException(String fieldLocation, String message) {
        super(message);
        this.fieldLocation = fieldLocation;
    }

    public String getFieldLocation() {
        return fieldLocation;
    }
}


