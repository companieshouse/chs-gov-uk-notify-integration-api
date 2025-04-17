package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.io.File;
import java.util.HashMap;
import java.util.Map;
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

    private final NotificationClient client;

    public GovUkNotifyService(NotificationClient client) {
        this.client = client;
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
            @NotNull File precompiledPdf
    ) {
        try {
            LetterResponse response = client.sendPrecompiledLetter(reference, precompiledPdf);
            return new LetterResp(response != null && response.getNotificationId() != null,
                    response);
        } catch (NotificationClientException nce) {
            Map<String, Object> logData = Map.of(
                    "reference", reference
            );
            LOGGER.error("Failed to send email", nce, new HashMap<>(logData));
            return new LetterResp(false, null);
        }
    }


}
