package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception;

import jakarta.validation.ValidationException;

/**
 * Exception thrown to indicate a problem with the information received in the
 * incoming letter sending request.
 */
public class LetterValidationException extends ValidationException {
    public LetterValidationException(String message) {
        super(message);
    }

}
