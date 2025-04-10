package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import java.util.List;
import java.util.Map;

import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.lang.NonNull;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.util.DataMap;

import static java.util.stream.Collectors.toList;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    /**
     * Map of message (part) substitutions to make message more meaningful to human readers.
     * Workaround for fact we probably cannot provide a more meaningful message in
     * the @Pattern annotation in code generated by OpenAPI Generator.
     */
    private static final Map<String, String>
            ERROR_MESSAGE_PARAMETER_NAME_SUBSTITUTIONS =
            Map.of("sendLetter.arg1", "context ID (X-Request-ID)");

    private final Logger LOGGER;

    public GlobalExceptionHandler(Logger logger) {
        this.LOGGER = logger;
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
            ConstraintViolationException cve) {
        var message = "Error in " + APPLICATION_NAMESPACE + ": "
                + buildMessage(cve.getMessage());
        LOGGER.error("Will handle error `" + message + "` by responding with 400 Bad Request.",
                getLogMap(message));
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(message);
    }

    @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            @NonNull MethodArgumentNotValidException manve,
            @NonNull HttpHeaders headers,
            @NonNull HttpStatusCode status,
            @NonNull WebRequest request) {
        if (manve.hasFieldErrors()) {
            var errors = manve.getFieldErrors().stream()
                    .map(this::buildMessage)
                    .sorted()
                    .collect(toList());
            var message = "Error(s) in " + APPLICATION_NAMESPACE + ": " + errors;
            LOGGER.error(message, getLogMap(errors));
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                    .body(message);
        }

        return super.handleMethodArgumentNotValid(manve, headers, status, request);
    }

    /**
     * Provides a more legible/meaningful substitution for part of a message originating from a
     * low level exception.
     * 
     * @param exceptionMessage the message provided by the intercepted exception
     * @return the sanitised version of the message
     */
    private String buildMessage(String exceptionMessage) {
        var newMessage = exceptionMessage;
        for (Map.Entry<String, String> entry :
                ERROR_MESSAGE_PARAMETER_NAME_SUBSTITUTIONS.entrySet()) {
            newMessage = newMessage.replace(entry.getKey(), entry.getValue());
        }
        return newMessage;
    }

    /**
     * Builds an intelligible error message from the {@link FieldError} provided.
     * @param fieldError the error to be reported
     * @return the error message
     */
    private String buildMessage(FieldError fieldError) {
        return fieldError.getObjectName() + " "
                + fieldError.getField() + " "
                + fieldError.getDefaultMessage();
    }

    private Map<String, Object> getLogMap(String error) {
        return new DataMap.Builder()
                .errorMessage(error)
                .build()
                .getLogMap();
    }

    private Map<String, Object> getLogMap(List<String> errors) {
        return new DataMap.Builder()
                .errors(errors)
                .build()
                .getLogMap();
    }

}
