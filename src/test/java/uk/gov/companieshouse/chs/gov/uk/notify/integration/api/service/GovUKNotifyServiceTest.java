package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import java.io.File;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import jakarta.validation.ConstraintViolationException;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit-test")
public class GovUKNotifyServiceTest {

    private GovUkNotifyService govUkNotifyService;

    @Mock
    private NotificationClient mockClient;

    @Mock
    private SendEmailResponse mockEmailResponse;

    @Mock
    private LetterResponse mockLetterResponse;

    @Mock
    private File mockPdf;

    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_TEMPLATE_ID = "valid-template-id";
    private static final String VALID_RECIPIENT = "Test Recipient";
    private static final Map<String, String> VALID_PERSONALISATION = Map.of("name", "Test User");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ProxyFactory factory = new ProxyFactory(new GovUkNotifyService("test-api-key"));
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        factory.addAdvice(new MethodValidationInterceptor(validator));
        govUkNotifyService = (GovUkNotifyService) factory.getProxy();

        ReflectionTestUtils.setField(govUkNotifyService, "client", mockClient);
    }

    @Nested
    @DisplayName("Email Sending Tests")
    class EmailTests {

        @Test
        @DisplayName("When_ValidEmailAndTemplateProvided_Expect_SendEmailSucceeds")
        void When_ValidEmailAndTemplateProvided_Expect_SendEmailSucceeds() throws NotificationClientException {
            UUID mockUuid = UUID.randomUUID();
            when(mockEmailResponse.getNotificationId()).thenReturn(mockUuid);
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(mockEmailResponse);

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);

            assertTrue(result.success());
            assertEquals(mockEmailResponse, result.response());
            verify(mockClient).sendEmail(eq(VALID_TEMPLATE_ID), eq(VALID_EMAIL), eq(VALID_PERSONALISATION), anyString());
        }

        @Test
        @DisplayName("When_ClientReturnsNullResponse_Expect_SendEmailReturnsFalse")
        void When_ClientReturnsNullResponse_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(null);

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);

            assertFalse(result.success());
            assertNull(result.response());
        }

        @Test
        @DisplayName("When_ResponseHasNullNotificationId_Expect_SendEmailReturnsFalse")
        void When_ResponseHasNullNotificationId_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockEmailResponse.getNotificationId()).thenReturn(null);
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(mockEmailResponse);

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);

            assertFalse(result.success());
            assertEquals(mockEmailResponse, result.response());
        }

        @Test
        @DisplayName("When_ClientThrowsException_Expect_SendEmailReturnsFalse")
        void When_ClientThrowsException_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
                    .thenThrow(new NotificationClientException("Test exception"));

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);

            assertFalse(result.success());
            assertNull(result.response());
        }

        @Test
        @DisplayName("When_NullPersonalisationProvided_Expect_SendEmailSucceeds")
        void When_NullPersonalisationProvided_Expect_SendEmailSucceeds() throws NotificationClientException {
            UUID mockUuid = UUID.randomUUID();
            when(mockEmailResponse.getNotificationId()).thenReturn(mockUuid);
            when(mockClient.sendEmail(anyString(), anyString(), isNull(), anyString())).thenReturn(mockEmailResponse);

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, null);

            assertTrue(result.success());
            assertEquals(mockEmailResponse, result.response());
            verify(mockClient).sendEmail(eq(VALID_TEMPLATE_ID), eq(VALID_EMAIL), isNull(), anyString());
        }
    }

    @Nested
    @DisplayName("Letter Sending Tests")
    class LetterTests {

        @Test
        @DisplayName("When_ValidRecipientAndPdfProvided_Expect_SendLetterSucceeds")
        void When_ValidRecipientAndPdfProvided_Expect_SendLetterSucceeds() throws NotificationClientException {
            UUID mockUuid = UUID.randomUUID();
            when(mockLetterResponse.getNotificationId()).thenReturn(mockUuid);
            when(mockClient.sendPrecompiledLetter(anyString(), any(File.class))).thenReturn(mockLetterResponse);

            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            assertTrue(result.success());
            assertEquals(mockLetterResponse, result.response());
            verify(mockClient).sendPrecompiledLetter(eq(VALID_RECIPIENT), eq(mockPdf));
        }

        @Test
        @DisplayName("When_ClientReturnsNullResponse_Expect_SendLetterReturnsFalse")
        void When_ClientReturnsNullResponse_Expect_SendLetterReturnsFalse() throws NotificationClientException {
            when(mockClient.sendPrecompiledLetter(anyString(), any(File.class))).thenReturn(null);

            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            assertFalse(result.success());
            assertNull(result.response());
        }

        @Test
        @DisplayName("When_ResponseHasNullNotificationId_Expect_SendLetterReturnsFalse")
        void When_ResponseHasNullNotificationId_Expect_SendLetterReturnsFalse() throws NotificationClientException {
            when(mockLetterResponse.getNotificationId()).thenReturn(null);
            when(mockClient.sendPrecompiledLetter(anyString(), any(File.class))).thenReturn(mockLetterResponse);

            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            assertFalse(result.success());
            assertEquals(mockLetterResponse, result.response());
        }

        @Test
        @DisplayName("When_ClientThrowsException_Expect_SendLetterReturnsFalse")
        void When_ClientThrowsException_Expect_SendLetterReturnsFalse() throws NotificationClientException {
            when(mockClient.sendPrecompiledLetter(anyString(), any(File.class)))
                    .thenThrow(new NotificationClientException("Test exception"));

            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            assertFalse(result.success());
            assertNull(result.response());
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class ValidationTests {

        @DisplayName("When_InvalidEmailProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "When invalid email: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"invalid-email", "missing-at.com", "@missinguser.com"})
        void When_EmailInvalidEmailProvided_Expect_ConstraintViolationException(String invalidEmail) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUkNotifyService.sendEmail(invalidEmail, VALID_TEMPLATE_ID, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_InvalidTemplateIdProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "When invalid template ID: {0}")
        @NullAndEmptySource
        void When_EmailInvalidTemplateIdProvided_Expect_ConstraintViolationException(String invalidTemplateId) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUkNotifyService.sendEmail(VALID_EMAIL, invalidTemplateId, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_MultipleInvalidInputsProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "Email: {0}, TemplateId: {1}")
        @MethodSource("provideInvalidInputCombinations")
        void When_EmailMultipleInvalidInputsProvided_Expect_ConstraintViolationException(
                String email, String templateId, String testDescription) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUkNotifyService.sendEmail(email, templateId, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_InvalidRecipientProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "When invalid recipient: {0}")
        @NullAndEmptySource
        void When_LetterInvalidRecipientProvided_Expect_ConstraintViolationException(String invalidRecipient) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUkNotifyService.sendLetter(invalidRecipient, mockPdf)
            );
        }

        @Test
        void When_LetterInvalidFileProvided_Expect_ConstraintViolationException() {
            assertThrows(ConstraintViolationException.class, () ->
                    govUkNotifyService.sendLetter(VALID_RECIPIENT, null)
            );
        }

        private static Stream<Arguments> provideInvalidInputCombinations() {
            return Stream.of(
                    Arguments.of("invalid-email", VALID_TEMPLATE_ID, "Invalid email with valid template"),
                    Arguments.of(VALID_EMAIL, "", "Valid email with empty template"),
                    Arguments.of("", VALID_TEMPLATE_ID, "Empty email with valid template"),
                    Arguments.of("invalid-email", "", "Invalid email with empty template")
            );
        }
    }
}
