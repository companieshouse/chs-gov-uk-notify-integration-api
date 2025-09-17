package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown to indicate that a sent letter (record) could not be found
 * in the database.
 */
@ResponseStatus(HttpStatus.NOT_FOUND)
public class LetterNotFoundException extends RuntimeException {
    public LetterNotFoundException(String message) {
        super(message);
    }
}
