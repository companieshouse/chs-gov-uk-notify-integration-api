package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;


@Service
public class GovUkNotifyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    public static final String ERROR_MESSAGE_KEY = "error";
    public static final UUID NIL_UUID = new UUID(0L, 0L);

    private final NotificationClient client;
    private final ObjectMapper objectMapper;

    public GovUkNotifyService(NotificationClient client, ObjectMapper objectMapper) {
        this.client = client;
        this.objectMapper = objectMapper;
    }

    public record EmailResp(boolean success, SendEmailResponse response) {
        // Empty: using only auto-generated methods
    }

    public EmailResp sendEmail(
            @NotBlank @Email String recipient,
            @NotBlank String templateId,
            Map<String, ?> personalisation) {
        String reference = recipient + "-" + System.currentTimeMillis();
        try {
            SendEmailResponse response = client.sendEmail(templateId, recipient, personalisation, reference);
            return new EmailResp(response != null && response.getNotificationId() != null, response);
        } catch (NotificationClientException e) {
            Map<String, Object> logData = Map.of(
                    "recipient", recipient,
                    "templateId", templateId
            );
            LOGGER.error("Failed to send email", e, new HashMap<>(logData));
            return new EmailResp(false, null);
        }
    }

    public record LetterResp(boolean success, LetterResponse response) {
        // Empty: using only auto-generated methods
    }

    public LetterResp sendLetter(
            @NotBlank String reference,
            @NotNull InputStream precompiledPdf) {
        try {
            var response =
                    client.sendPrecompiledLetterWithInputStream(reference, precompiledPdf);
            return new LetterResp(response != null && response.getNotificationId() != null,
                    response);
        } catch (NotificationClientException nce) {
            var logData = Map.of("reference", reference);
            LOGGER.error("Failed to send letter", nce, new HashMap<>(logData));
            try {
                var response = buildLetterResponseForError(nce, reference);
                return new LetterResp(false, response);
            } catch (JsonProcessingException jpe) {
                LOGGER.error("Failed to build error response", jpe, new HashMap<>(logData));
            }
            return new LetterResp(false, null);
        }
    }

    /**
     * Builds a LetterResponse containing useful information about the error reported by the
     * NotificationClientException caught.
     *
     * @param nce the exception caught
     * @param reference the letter (sender details) reference
     * @return a LetterResponse, which, if stored in the responses collection, may help
     *         troubleshooting
     * @throws JsonProcessingException should there be a problem converting the id and reference
     *         provided nto JSON (unlikely)
     */
    private LetterResponse buildLetterResponseForError(
            NotificationClientException nce, String reference)
        throws JsonProcessingException {
        var responseData = Map.of(
                // id is required because we are using LetterResponse to capture info (for storage).
                // We might consider just using a different object (or a map) to avoid this,
                // but that would require a bigger rework.
                "id", NIL_UUID, // Make it clear this is NOT a notification ID issued by Gov Notify.
                "reference", reference
        );
        var jsonData = objectMapper.writeValueAsString(responseData);
        var response = new LetterResponse(jsonData);
        response.getData().put(ERROR_MESSAGE_KEY, nce.getMessage());
        return response;
    }


}
