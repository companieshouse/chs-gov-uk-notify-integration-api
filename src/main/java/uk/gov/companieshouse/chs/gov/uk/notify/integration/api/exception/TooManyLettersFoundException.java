package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

/**
 * Exception thrown to indicate that multiple sent letters (records) were found to meet the
 * query criteria provided in a context in which only a single match should be found.
 */
@ResponseStatus(HttpStatus.CONFLICT)
public class TooManyLettersFoundException extends RuntimeException {
    public TooManyLettersFoundException(String message) {
        super(message);
    }
}
