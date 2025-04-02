package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.SenderDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.EmailDetails;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.RecipientDetailsEmail;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailgovuknotifypayload.EmailGovUkNotifyPayloadInterface;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.CREATED;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class SenderRestApiTests {

    @Mock
    EmailGovUkNotifyPayloadInterface emailGovUkNotifyPayloadInterface;

    @InjectMocks
    private SenderRestApi restApi;


    @Test
    public void validEmailRequest() {
        String xHeaderId = "1";
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        EmailDetails emailDetails = new EmailDetails();
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        SenderDetails senderDetails = new SenderDetails();

        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@email.address.net")
                .userId("9876543")
                .name("John Doe")
                .reference("ref")
                .appId("chips.send_email"));
        govUkEmailDetailsRequest.setEmailDetails(emailDetails
                .templateId("template_id")
                .templateVersion(BigDecimal.valueOf(1))
                .personalisationDetails("letter_reference: 0123456789,company_name: BIG SHOP LTD,company_id: 9876543210,psc_type: 25% "));
        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress("john.doe@email.address.net")
                .name("john doe"));

        emailGovUkNotifyPayloadInterface.sendEmail(govUkEmailDetailsRequest);

        ResponseEntity<Void> response = restApi.sendEmail(govUkEmailDetailsRequest, xHeaderId);

        assertThat(response.getStatusCode()).isEqualTo(CREATED);
        Assertions.assertNotNull(response);
    }

    @Test
    public void senderDetailEmailAddressEmpty() {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            String xHeaderId = "1";
            MockHttpServletRequest request = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            SenderDetails senderDetails = new SenderDetails();
            EmailDetails emailDetails = new EmailDetails();
            RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
            GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();

            govUkEmailDetailsRequest.setSenderDetails(senderDetails
                    .emailAddress(""));
            govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                    .emailAddress("john.doe@email.address.net")
                    .name("john doe"));
            govUkEmailDetailsRequest.setEmailDetails(emailDetails
                    .templateId("template_id")
                    .templateVersion(BigDecimal.valueOf(1))
                    .personalisationDetails("letter_reference: 0123456789,company_name: BIG SHOP LTD,company_id: 9876543210,psc_type: 25% "));

            emailGovUkNotifyPayloadInterface.sendEmail(govUkEmailDetailsRequest);

            ResponseEntity<Void> response = restApi.sendEmail(govUkEmailDetailsRequest, xHeaderId);

            assertThat(response).isEqualTo("Sender Email Address is empty");
            assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        });
        Assertions.assertEquals("Sender Email Address is empty", thrown.getMessage());
    }

    @Test
    public void recipientDetailsEmailAddressEmpty() {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            String xHeaderId = "1";
            MockHttpServletRequest request = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            SenderDetails senderDetails = new SenderDetails();
            RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
            EmailDetails emailDetails = new EmailDetails();
            GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();

            govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                    .emailAddress(""));
            govUkEmailDetailsRequest.setEmailDetails(emailDetails
                            .templateId("template_id")
                            .templateVersion(BigDecimal.valueOf(1))
                            .personalisationDetails("letter_reference: 0123456789,company_name: BIG SHOP LTD,company_id: 9876543210,psc_type: 25% "));
            govUkEmailDetailsRequest.setSenderDetails(senderDetails
                    .emailAddress("john.doe@email.address.net"));

            ResponseEntity<Void> response = restApi.sendEmail(govUkEmailDetailsRequest, xHeaderId);

            assertThat(response).isEqualTo("Sender Email Address is empty");
            assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        });
        Assertions.assertEquals("Sender Email Address is empty", thrown.getMessage());
    }

    @Test
    public void invalidEmailRequestNullEmailDetails() {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
        String xHeaderId = "1";
        MockHttpServletRequest request = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

        SenderDetails senderDetails = new SenderDetails();
        RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
        GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();

        govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                .emailAddress("john.doe@email.address.net"));
        govUkEmailDetailsRequest.setSenderDetails(senderDetails
                .emailAddress("john.doe@email.address.net"));
        govUkEmailDetailsRequest.setEmailDetails(null);
        emailGovUkNotifyPayloadInterface.sendEmail(govUkEmailDetailsRequest);

        ResponseEntity<Void> response = restApi.sendEmail(govUkEmailDetailsRequest, xHeaderId);

        assertThat(response).isEqualTo("Sender request has null fields");
        assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        });
        Assertions.assertEquals("Sender request has null fields", thrown.getMessage());
    }
    @Test
    public void invalidEmailRequestNullRecipientDetails() {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            String xHeaderId = "1";
            MockHttpServletRequest request = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            SenderDetails senderDetails = new SenderDetails();
            EmailDetails emailDetails = new EmailDetails();
            GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();

            govUkEmailDetailsRequest.setRecipientDetails(null);
            govUkEmailDetailsRequest.setSenderDetails(senderDetails
                    .emailAddress("john.doe@email.address.net"));
            govUkEmailDetailsRequest.setEmailDetails(emailDetails
                    .templateId("template_id")
                    .templateVersion(BigDecimal.valueOf(1))
                    .personalisationDetails("letter_reference: 0123456789,company_name: BIG SHOP LTD,company_id: 9876543210,psc_type: 25% "));
            emailGovUkNotifyPayloadInterface.sendEmail(govUkEmailDetailsRequest);

            ResponseEntity<Void> response = restApi.sendEmail(govUkEmailDetailsRequest, xHeaderId);

            assertThat(response).isEqualTo("Sender request has null fields");
            assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        });
        Assertions.assertEquals("Sender request has null fields", thrown.getMessage());
    }

    @Test
    public void invalidEmailRequestAllFieldsNull() {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            String xHeaderId = "1";
            MockHttpServletRequest request = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request));

            GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();

            govUkEmailDetailsRequest.setRecipientDetails(null);
            govUkEmailDetailsRequest.setSenderDetails(null);
            govUkEmailDetailsRequest.setEmailDetails(null);
            emailGovUkNotifyPayloadInterface.sendEmail(govUkEmailDetailsRequest);

            ResponseEntity<Void> response = restApi.sendEmail(govUkEmailDetailsRequest, xHeaderId);
            assertThat(response).isEqualTo("Sender request has null fields");
            assertThat(response.getStatusCode()).isEqualTo(BAD_REQUEST);
        });
        Assertions.assertEquals("Sender request has null fields", thrown.getMessage());
    }


}
