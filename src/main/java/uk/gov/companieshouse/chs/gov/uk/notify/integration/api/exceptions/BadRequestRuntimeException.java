package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions;

public class BadRequestRuntimeException extends RuntimeException {

    public BadRequestRuntimeException(String message) {
        super(message);
    }
}
