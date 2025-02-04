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
class YourRoleAtAcspHasChangedEmailDataTest {

    @Value("${signin.url}")
    private String signinUrl;

    @Test
    void canConstructEmailDataWithBuilderPatternApproach() {
        final var emailData = new YourRoleAtAcspHasChangedToOwnerEmailData()
                .to("buzz.lightyear@toystory.com")
                .subject("Space Ranger Promotion")
                .editedBy("Woody")
                .acspName("Netflix")
                .signinUrl(signinUrl);

        Assertions.assertEquals("buzz.lightyear@toystory.com", emailData.getTo());
        Assertions.assertEquals("Space Ranger Promotion", emailData.getSubject());
        Assertions.assertEquals("Woody", emailData.getEditedBy());
        Assertions.assertEquals("Netflix", emailData.getAcspName());
        Assertions.assertEquals(signinUrl, emailData.getSigninUrl());

    }

    @Test
    void canConstructEmailDataWithTraditionalApproach() {
        final var emailData = new YourRoleAtAcspHasChangedToOwnerEmailData();
        emailData.setTo("buzz.lightyear@toystory.com");
        emailData.setSubject("Space Ranger Promotion");
        emailData.setEditedBy("Woody");
        emailData.setAcspName("Netflix");
        emailData.setSigninUrl(signinUrl);

        Assertions.assertEquals("buzz.lightyear@toystory.com", emailData.getTo());
        Assertions.assertEquals("Space Ranger Promotion", emailData.getSubject());
        Assertions.assertEquals("Woody", emailData.getEditedBy());
        Assertions.assertEquals("Netflix", emailData.getAcspName());
        Assertions.assertEquals(signinUrl, emailData.getSigninUrl());
    }

    @Test
    void canConstructEmailDataWithConstructor() {
        final var emailData = new YourRoleAtAcspHasChangedToOwnerEmailData("buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl);
        Assertions.assertEquals("buzz.lightyear@toystory.com", emailData.getTo());
        Assertions.assertEquals("Your role for Netflix has changed", emailData.getSubject());
        Assertions.assertEquals("Woody", emailData.getEditedBy());
        Assertions.assertEquals("Netflix", emailData.getAcspName());
        Assertions.assertEquals(signinUrl, emailData.getSigninUrl());
    }

    @Test
    void subjectSetsDerivedSubject() {
        final var emailData = new YourRoleAtAcspHasChangedToOwnerEmailData()
                .to("buzz.lightyear@toystory.com")
                .subject("Space Ranger Promotion")
                .editedBy("Woody")
                .acspName("Netflix")
                .subject()
                .signinUrl(signinUrl);

        Assertions.assertEquals("Your role for Netflix has changed", emailData.getSubject());
    }

    @Test
    void subjectWithoutAcspNameThrowsNullPointerException() {
        final var emailData = new YourRoleAtAcspHasChangedToOwnerEmailData();
        Assertions.assertThrows(NullPointerException.class, emailData::subject);
    }

    @Test
    void equalsReturnsTrueWhenEmailDataAreEquivalentOtherwiseFalse() {
        final Supplier<YourRoleAtAcspHasChangedToOwnerEmailData> buzzEmailSupplier = () -> new YourRoleAtAcspHasChangedToOwnerEmailData("buzz.lightyear@toystory.com", "Woody", "Netflix", signinUrl);
        final var potatoHeadEmail = new YourRoleAtAcspHasChangedToOwnerEmailData("potato.head@toystory.com", "Woody", "Netflix", signinUrl);
        Assertions.assertEquals(buzzEmailSupplier.get(), buzzEmailSupplier.get());
        Assertions.assertNotEquals(potatoHeadEmail, buzzEmailSupplier.get());
    }

}
