package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception;

public class EmailClientException extends RuntimeException {

    public EmailClientException(String message) {
        super(message);
    }

    public EmailClientException(String message, Throwable cause) {
        super(message, cause);
    }
}
