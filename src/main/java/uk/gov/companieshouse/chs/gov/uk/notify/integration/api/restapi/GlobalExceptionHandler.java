package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.companieshouse.logging.Logger;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    private final Logger logger;

    public GlobalExceptionHandler(Logger logger) {
        this.logger = logger;
    }

    /**
     * Returns HTTP Status 400 Bad Request when there is an exception implying that
     * the incoming request content is invalid.
     *
     * @param cve exception thrown when input from request is found to be invalid
     * @return response with payload reporting underlying cause
     */
    @ExceptionHandler(ConstraintViolationException.class)
    public ResponseEntity<Object> handleConstraintViolationException(
            final ConstraintViolationException cve) {
        final String message = "Error in " + APPLICATION_NAMESPACE + ": " + cve.getMessage();
        logger.error("Will handle error `" + message + "` by responding with 401 Bad Request.");
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(message);
    }
}
