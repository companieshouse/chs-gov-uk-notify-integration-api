package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import org.junit.jupiter.api.Assertions;
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
import static org.springframework.http.HttpStatus.CREATED;

@ExtendWith(MockitoExtension.class)
public class SenderRestApiTests {

    @Mock
    EmailGovUkNotifyPayloadInterface emailGovUkNotifyPayloadInterface;

    @InjectMocks
    private SenderRestApi restApi;


    @Test
    public void validEmailRequest() {

        String xHeaderId = "1";
        MockHttpServletRequest request1 = new MockHttpServletRequest();
        RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request1));

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
    public void invalidEmailRequest() {
        RuntimeException thrown = Assertions.assertThrows(RuntimeException.class, () -> {
            String xRequestId = "1";
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request1));

            SenderDetails senderDetails = new SenderDetails();
            GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();

            govUkEmailDetailsRequest.setSenderDetails(senderDetails
                    .emailAddress(""));

            ResponseEntity<Void> response = restApi.sendEmail(govUkEmailDetailsRequest, xRequestId);
            throw new IllegalArgumentException("Sender Email Address is empty");
        });
        Assertions.assertEquals("Sender Email Address is empty", thrown.getMessage());
    }

    @Test
    public void invalidEmailRequest1() {
        RuntimeException newThrown = Assertions.assertThrows(RuntimeException.class, () -> {
            String xRequestId = "1";
            MockHttpServletRequest request1 = new MockHttpServletRequest();
            RequestContextHolder.setRequestAttributes(new ServletRequestAttributes(request1));

            SenderDetails senderDetails = new SenderDetails();
            RecipientDetailsEmail recipientDetailsEmail = new RecipientDetailsEmail();
            GovUkEmailDetailsRequest govUkEmailDetailsRequest = new GovUkEmailDetailsRequest();

            govUkEmailDetailsRequest.setRecipientDetails(recipientDetailsEmail
                    .emailAddress(""));
            govUkEmailDetailsRequest.setSenderDetails(senderDetails
                    .emailAddress("john.doe@email.address.net"));

            ResponseEntity<Void> response = restApi.sendEmail(govUkEmailDetailsRequest, xRequestId);
            throw new IllegalArgumentException("Sender Email Address is empty");
        });
        Assertions.assertEquals("Sender Email Address is empty", newThrown.getMessage());

    }
}
