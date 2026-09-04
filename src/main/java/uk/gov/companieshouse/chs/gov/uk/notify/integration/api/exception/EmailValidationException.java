package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception;

import jakarta.validation.ValidationException;

public class EmailValidationException extends ValidationException {

    public EmailValidationException(String message) {
        super(message);
    }

}
