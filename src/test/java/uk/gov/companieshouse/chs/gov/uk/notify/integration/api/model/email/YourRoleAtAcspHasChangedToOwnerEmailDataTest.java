package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class YourRoleAtAcspHasChangedToOwnerEmailDataTest {

    @Value("${signin.url}")
    private String signinUrl;

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage() {
        final var message = new YourRoleAtAcspHasChangedToOwnerEmailData("buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl).toNotificationSentLoggingMessage();
        Assertions.assertEquals("your_role_at_acsp_has_changed_to_owner notification sent. buzz.lightyear@toystory.com role at Netflix was changed by Woody.", message);
    }

}
