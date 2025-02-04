package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;

import java.util.function.Supplier;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class ConfirmYouAreAMemberEmailDataTest {

    @Value("${signin.url}")
    private String signinUrl;

    @Test
    void canConstructEmailDataWithBuilderPatternApproach() {
        final var emailData = new ConfirmYouAreAnOwnerMemberEmailData()
                .to("buzz.lightyear@toystory.com")
                .subject("Space Ranger Promotion")
                .addedBy("Woody")
                .acspName("Netflix")
                .signinUrl(signinUrl);

        Assertions.assertEquals("buzz.lightyear@toystory.com", emailData.getTo());
        Assertions.assertEquals("Space Ranger Promotion", emailData.getSubject());
        Assertions.assertEquals("Woody", emailData.getAddedBy());
        Assertions.assertEquals("Netflix", emailData.getAcspName());
        Assertions.assertEquals(signinUrl, emailData.getSigninUrl());
    }

    @Test
    void canConstructEmailDataWithTraditionalApproach() {
        final var emailData = new ConfirmYouAreAnOwnerMemberEmailData();
        emailData.setTo("buzz.lightyear@toystory.com");
        emailData.setSubject("Space Ranger Promotion");
        emailData.setAddedBy("Woody");
        emailData.setAcspName("Netflix");
        emailData.setSigninUrl(signinUrl);

        Assertions.assertEquals("buzz.lightyear@toystory.com", emailData.getTo());
        Assertions.assertEquals("Space Ranger Promotion", emailData.getSubject());
        Assertions.assertEquals("Woody", emailData.getAddedBy());
        Assertions.assertEquals("Netflix", emailData.getAcspName());
        Assertions.assertEquals(signinUrl, emailData.getSigninUrl());
    }

    @Test
    void canConstructEmailDataWithConstructor() {
        final var emailData = new ConfirmYouAreAnOwnerMemberEmailData("buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl);
        Assertions.assertEquals("buzz.lightyear@toystory.com", emailData.getTo());
        Assertions.assertEquals("You have been added to a Companies House authorised agent", emailData.getSubject());
        Assertions.assertEquals("Woody", emailData.getAddedBy());
        Assertions.assertEquals("Netflix", emailData.getAcspName());
        Assertions.assertEquals(signinUrl, emailData.getSigninUrl());
    }

    @Test
    void subjectSetsDerivedSubject() {
        final var emailData = new ConfirmYouAreAnOwnerMemberEmailData()
                .to("buzz.lightyear@toystory.com")
                .subject("Space Ranger Promotion")
                .addedBy("Woody")
                .acspName("Netflix")
                .signinUrl(signinUrl)
                .subject();

        Assertions.assertEquals("You have been added to a Companies House authorised agent", emailData.getSubject());
    }

    @Test
    void equalsReturnsTrueWhenEmailDataAreEquivalentOtherwiseFalse() {
        final Supplier<ConfirmYouAreAnOwnerMemberEmailData> buzzEmailSupplier = () -> new ConfirmYouAreAnOwnerMemberEmailData("buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl);
        final var potatoHeadEmail = new ConfirmYouAreAnOwnerMemberEmailData("potato.head@toystory.com", "Woody", "Netflix", signinUrl);
        Assertions.assertEquals(buzzEmailSupplier.get(), buzzEmailSupplier.get());
        Assertions.assertNotEquals(potatoHeadEmail, buzzEmailSupplier.get());
    }

}
