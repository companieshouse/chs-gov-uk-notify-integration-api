package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailfacade;

import java.util.Map;
import java.util.concurrent.CompletableFuture;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import org.springframework.validation.annotation.Validated;

@Validated
public interface EmailFacadeInterface {

    boolean sendEmail(
            @NotBlank @Email String recipient,
            @NotBlank String templateId,
            Map<String, ?> personalisation);
    
    CompletableFuture<Boolean> sendEmailAsync(
            @NotBlank @Email String recipient,
            @NotBlank String templateId,
            Map<String, ?> personalisation);
}
