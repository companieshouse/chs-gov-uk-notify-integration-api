package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception;

public class EmailNotFoundException extends RuntimeException {

    public EmailNotFoundException(String message) {
        super(message);
    }

}
