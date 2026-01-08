package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception;

import jakarta.validation.ValidationException;

/**
 * Exception thrown to indicate a problem with the information received in the
 * incoming view letter(s) request.
 */
public class ViewLetterValidationException  extends ValidationException  {
    public ViewLetterValidationException(String message) {
        super(message);
    }
}
