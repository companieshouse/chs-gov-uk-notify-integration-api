package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class ConfirmYouAreAnOwnerMemberEmailDataTest {

    @Value("${signin.url}")
    private String signinUrl;

    @Test
    void toNotificationSentLoggingMessageComputesCorrectMessage() {
        final var message = new ConfirmYouAreAnOwnerMemberEmailData("buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl).toNotificationSentLoggingMessage();
        Assertions.assertEquals("confirm_you_are_an_owner_member notification sent. buzz.lightyear@toystory.com was added to Netflix by Woody.", message);
    }

}
