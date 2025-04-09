package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

@Service
public class GovUkNotifyService {
    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    private final NotificationClient client;

    public GovUkNotifyService(
            @Value("${gov.uk.notify.api.key}") final String apiKey
    ) {
        this.client = new NotificationClient(apiKey);
    }

    public record EmailResp(boolean success, SendEmailResponse response) {
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
    }

    public LetterResp sendLetter(
            @NotBlank String recipient,
            @NotNull File precompiledPdf
    ) {
        try {
            LetterResponse response = client.sendPrecompiledLetter(recipient, precompiledPdf);
            return new LetterResp(response != null && response.getNotificationId() != null, response);
        } catch (NotificationClientException e) {
            Map<String, Object> logData = Map.of(
                    "recipient", recipient
            );
            LOGGER.error("Failed to send email", e, new HashMap<>(logData));
            return new LetterResp(false, null);
        }
    }


}
