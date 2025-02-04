package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.annotation.Value;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.UserRoleEnum;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email.*;
import uk.gov.companieshouse.email_producer.EmailProducer;
import uk.gov.companieshouse.email_producer.EmailSendingException;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.MessageType.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class EmailServiceTest {

    @Mock
    private EmailProducer emailProducer;

    @InjectMocks
    private EmailService emailService;

    @Value("${signin.url}")
    private String signinUrl;

    @Test
    void sendConfirmYouAreAMemberEmailWithNullRecipientEmailOrAddedByOrAcspNameOrRoleThrowsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail("theId123", null, "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER));
        Assertions.assertThrows(IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", null, "Witcher", UserRoleEnum.OWNER));
        Assertions.assertThrows(IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", null, UserRoleEnum.OWNER));
        Assertions.assertThrows(IllegalArgumentException.class, () -> emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", null));
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithUnexpectedIssueThrowsEmailSendingException() {
        Mockito.doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue()));
        Mockito.doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue()));
        Mockito.doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue()));

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD));
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToOwnerThrowsMessageOnToKafkaQueue() {
        final var expectedEmailData = new ConfirmYouAreAnOwnerMemberEmailData("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl);
        emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER);
        Mockito.verify(emailProducer).sendEmail(expectedEmailData, CONFIRM_YOU_ARE_AN_OWNER_MEMBER_MESSAGE_TYPE.getValue());
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToAdminThrowsMessageOnToKafkaQueue() {
        final var expectedEmailData = new ConfirmYouAreAnAdminMemberEmailData("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl);
        emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN);
        Mockito.verify(emailProducer).sendEmail(expectedEmailData, CONFIRM_YOU_ARE_AN_ADMIN_MEMBER_MESSAGE_TYPE.getValue());
    }

    @Test
    void sendConfirmYouAreAMemberEmailWithRoleSetToStandardThrowsMessageOnToKafkaQueue() {
        final var expectedEmailData = new ConfirmYouAreAStandardMemberEmailData("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl);
        emailService.sendConfirmYouAreAMemberEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD);
        Mockito.verify(emailProducer).sendEmail(expectedEmailData, CONFIRM_YOU_ARE_A_STANDARD_MEMBER_MESSAGE_TYPE.getValue());
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNullRecipientEmailOrEditedByOrAcspNameOrNewRoleThrowsIllegalArgumentException() {
        Assertions.assertThrows(IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("theId123", null, "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER));
        Assertions.assertThrows(IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", null, "Witcher", UserRoleEnum.OWNER));
        Assertions.assertThrows(IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", null, UserRoleEnum.OWNER));
        Assertions.assertThrows(IllegalArgumentException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", null));
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithUnexpectedIssueThrowsEmailSendingException() {
        Mockito.doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue()));
        Mockito.doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue()));
        Mockito.doThrow(new EmailSendingException("Failed to send email", new Exception())).when(emailProducer).sendEmail(any(), eq(YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE.getValue()));

        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN));
        Assertions.assertThrows(EmailSendingException.class, () -> emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD));
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToOwnerThrowsMessageOnToKafkaQueue() {
        final var expectedEmailData = new YourRoleAtAcspHasChangedToOwnerEmailData("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl);
        emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.OWNER);
        Mockito.verify(emailProducer).sendEmail(expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_OWNER_MESSAGE_TYPE.getValue());
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToAdminThrowsMessageOnToKafkaQueue() {
        final var expectedEmailData = new YourRoleAtAcspHasChangedToAdminEmailData("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl);
        emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.ADMIN);
        Mockito.verify(emailProducer).sendEmail(expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_ADMIN_MESSAGE_TYPE.getValue());
    }

    @Test
    void sendYourRoleAtAcspHasChangedEmailWithNewRoleSetToStandardThrowsMessageOnToKafkaQueue() {
        final var expectedEmailData = new YourRoleAtAcspHasChangedToStandardEmailData("buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", signinUrl);
        emailService.sendYourRoleAtAcspHasChangedEmail("theId123", "buzz.lightyear@toystory.com", "demo@ch.gov.uk", "Witcher", UserRoleEnum.STANDARD);
        Mockito.verify(emailProducer).sendEmail(expectedEmailData, YOUR_ROLE_AT_ACSP_HAS_CHANGED_TO_STANDARD_MESSAGE_TYPE.getValue());
    }

}
