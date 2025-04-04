package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailfacade;

import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;

import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Service
public class GovUKNotifyEmailFacade {
    private final Logger logger = LoggerFactory.getLogger(GovUKNotifyEmailFacade.class.getName());
    private final NotificationClient client;

    /**
     * Constructor with dependency injection
     *
     * @param apiKey The GOV.UK Notify API key
     */
    public GovUKNotifyEmailFacade(
            @Value("${gov.uk.notify.api.key}") String apiKey) {
        this.client = new NotificationClient(apiKey);
        this.logger.info("NotifyEmailFacade initialized");
    }

    /**
     * Sends an email
     *
     * @param recipient       The recipient's email address
     * @param templateId      The GOV.UK Notify template ID to use
     * @param personalisation Map of template personalization values
     * @return true if the email was sent successfully, false otherwise
     */
    public boolean sendEmail(
            @NotBlank @Email String recipient,
            @NotBlank String templateId,
            Map<String, ?> personalisation) {
        try {
            SendEmailResponse response = sendEmailInternal(recipient, templateId, personalisation);
            return isSuccess(response);
        } catch (NotificationClientException e) {
            Map<String, Object> logData = Map.of(
                    "recipient", recipient,
                    "templateId", templateId
            );
            logger.error("Failed to send email", e, new HashMap<>(logData));
            return false;
        }
    }

    /**
     * Sends an email asynchronously
     *
     * @param recipient       The recipient's email address
     * @param templateId      The GOV.UK Notify template ID to use
     * @param personalisation Map of template personalization values
     * @return A future that will complete with true if successful, false otherwise
     */
    public CompletableFuture<Boolean> sendEmailAsync(
            @NotBlank @Email String recipient,
            @NotBlank String templateId,
            Map<String, ?> personalisation) {
        return CompletableFuture.supplyAsync(() -> sendEmail(recipient, templateId, personalisation));
    }
    
    private SendEmailResponse sendEmailInternal(
            String recipient,
            String templateId,
            Map<String, ?> personalisation) throws NotificationClientException {
        String reference = generateReference(recipient);
        return client.sendEmail(templateId, recipient, personalisation, reference);
    }
    
    private boolean isSuccess(SendEmailResponse response) {
        return response != null && response.getNotificationId() != null;
    }
    
    private String generateReference(String recipient) {
        return recipient + "-" + System.currentTimeMillis();
    }
}
