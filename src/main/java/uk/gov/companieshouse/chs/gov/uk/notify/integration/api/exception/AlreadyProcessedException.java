package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception;

public class AlreadyProcessedException extends RuntimeException {

    public AlreadyProcessedException(String message) {
        super(message);
    }
}
