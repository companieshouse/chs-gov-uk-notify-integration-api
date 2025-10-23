package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import java.io.InputStream;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Stream;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
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
import org.mockito.Spy;
import org.springframework.aop.framework.ProxyFactory;
import org.springframework.validation.beanvalidation.MethodValidationInterceptor;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService.ERROR_MESSAGE_KEY;

@Tag("unit-test")
public class GovUKNotifyServiceTest {

    private GovUkNotifyService govUkNotifyService;

    @Mock
    private NotificationClient mockClient;

    @Mock
    private NotificationClient mockMappedClient;

    @Mock
    private ApiKeyMappingService apiKeyMappingService;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Mock
    private SendEmailResponse mockEmailResponse;

    @Mock
    private LetterResponse mockLetterResponse;

    @Mock
    private InputStream mockPdf;

    @Mock
    private JsonProcessingException mockJsonProcessingException;

    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_TEMPLATE_ID = "valid-template-id";
    private static final String VALID_REFERENCE = "valid-reference";
    private static final String VALID_RECIPIENT = "Test Recipient";
    private static final Map<String, String> VALID_PERSONALISATION = Map.of("name", "Test User");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ProxyFactory factory = new ProxyFactory(new GovUkNotifyService(mockClient, objectMapper, apiKeyMappingService));
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        factory.addAdvice(new MethodValidationInterceptor(validator));
        govUkNotifyService = (GovUkNotifyService) factory.getProxy();

        // By default, no API key mapping matches
        when(apiKeyMappingService.findMatchingApiKey(anyString())).thenReturn(null);
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

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION);

            assertTrue(result.success());
            assertEquals(mockEmailResponse, result.response());
            verify(mockClient).sendEmail(eq(VALID_TEMPLATE_ID), eq(VALID_EMAIL), eq(VALID_PERSONALISATION), anyString());
        }

        @Test
        @DisplayName("When_ClientReturnsNullResponse_Expect_SendEmailReturnsFalse")
        void When_ClientReturnsNullResponse_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(null);

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION);

            assertFalse(result.success());
            assertNull(result.response());
        }

        @Test
        @DisplayName("When_ResponseHasNullNotificationId_Expect_SendEmailReturnsFalse")
        void When_ResponseHasNullNotificationId_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockEmailResponse.getNotificationId()).thenReturn(null);
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(mockEmailResponse);

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION);

            assertFalse(result.success());
            assertEquals(mockEmailResponse, result.response());
        }

        @Test
        @DisplayName("When_ClientThrowsException_Expect_SendEmailReturnsFalse")
        void When_ClientThrowsException_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
                    .thenThrow(new NotificationClientException("Test exception"));

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION);

            assertFalse(result.success());
            assertNull(result.response());
        }

        @Test
        @DisplayName("When_NullPersonalisationProvided_Expect_SendEmailSucceeds")
        void When_NullPersonalisationProvided_Expect_SendEmailSucceeds() throws NotificationClientException {
            UUID mockUuid = UUID.randomUUID();
            when(mockEmailResponse.getNotificationId()).thenReturn(mockUuid);
            when(mockClient.sendEmail(anyString(), anyString(), isNull(), anyString())).thenReturn(mockEmailResponse);

            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, null);

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
            when(mockClient.sendPrecompiledLetterWithInputStream(
                    anyString(), any(InputStream.class))).thenReturn(mockLetterResponse);

            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            assertTrue(result.success());
            assertEquals(mockLetterResponse, result.response());
            verify(mockClient).sendPrecompiledLetterWithInputStream(VALID_RECIPIENT, mockPdf);
        }

        @Test
        @DisplayName("When_ClientReturnsNullResponse_Expect_SendLetterReturnsFalse")
        void When_ClientReturnsNullResponse_Expect_SendLetterReturnsFalse() throws NotificationClientException {
            when(mockClient.sendPrecompiledLetterWithInputStream(
                    anyString(), any(InputStream.class))).thenReturn(null);

            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            assertFalse(result.success());
            assertNull(result.response());
        }

        @Test
        @DisplayName("When_ResponseHasNullNotificationId_Expect_SendLetterReturnsFalse")
        void When_ResponseHasNullNotificationId_Expect_SendLetterReturnsFalse() throws NotificationClientException {
            when(mockLetterResponse.getNotificationId()).thenReturn(null);
            when(mockClient.sendPrecompiledLetterWithInputStream(anyString(),
                    any(InputStream.class))).thenReturn(mockLetterResponse);

            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            assertFalse(result.success());
            assertEquals(mockLetterResponse, result.response());
        }

        @Test
        @DisplayName("When_ClientThrowsException_Expect_SendLetterReturnsFalse")
        void When_ClientThrowsException_Expect_SendLetterReturnsFalse() throws NotificationClientException {
            when(mockClient.sendPrecompiledLetterWithInputStream(anyString(), any(InputStream.class)))
                    .thenThrow(new NotificationClientException("Test exception"));

            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            assertFalse(result.success());
            assertNotNull(result.response());
            assertNotNull(result.response().getData());
            assertEquals("Test exception", result.response().getData().get(ERROR_MESSAGE_KEY));
        }

        @Test
        @DisplayName("Error encountered building letter response for error is handled gracefully")
        void errorEncounteredBuildingLetterResponseForErrorIsHandledGracefully()
                throws NotificationClientException, JsonProcessingException {
            when(mockClient.sendPrecompiledLetterWithInputStream(anyString(),
                    any(InputStream.class)))
                    .thenThrow(new NotificationClientException("Test exception"));
            when(objectMapper.writeValueAsString(anyMap())).thenThrow(mockJsonProcessingException);

            GovUkNotifyService.LetterResp result =
                    govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

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
                    govUkNotifyService.sendEmail(invalidEmail, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_InvalidTemplateIdProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "When invalid template ID: {0}")
        @NullAndEmptySource
        void When_EmailInvalidTemplateIdProvided_Expect_ConstraintViolationException(String invalidTemplateId) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUkNotifyService.sendEmail(VALID_EMAIL, invalidTemplateId, VALID_REFERENCE, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_MultipleInvalidInputsProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "Email: {0}, TemplateId: {1}")
        @MethodSource("provideInvalidInputCombinations")
        void When_EmailMultipleInvalidInputsProvided_Expect_ConstraintViolationException(
                String email, String templateId, String testDescription) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUkNotifyService.sendEmail(email, templateId, VALID_REFERENCE, VALID_PERSONALISATION)
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

    @Nested
    @DisplayName("API Key Mapping Tests")
    class ApiKeyMappingTests {

        @Test
        @DisplayName("When_NoMappingMatches_Expect_DefaultClientUsedForEmail")
        void When_NoMappingMatches_Expect_DefaultClientUsedForEmail() throws NotificationClientException {
            // Given
            UUID mockUuid = UUID.randomUUID();
            when(mockEmailResponse.getNotificationId()).thenReturn(mockUuid);
            when(apiKeyMappingService.findMatchingApiKey(VALID_REFERENCE)).thenReturn(null);
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(mockEmailResponse);

            // When
            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(
                VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION
            );

            // Then
            assertTrue(result.success());
            verify(apiKeyMappingService).findMatchingApiKey(VALID_REFERENCE);
            verify(mockClient).sendEmail(VALID_TEMPLATE_ID, VALID_EMAIL, VALID_PERSONALISATION, VALID_REFERENCE);
        }

        @Test
        @DisplayName("When_MappingMatches_Expect_MappedClientUsedForEmail")
        void When_MappingMatches_Expect_MappedClientUsedForEmail() throws NotificationClientException {
            // Given
            String mappedApiKey = "mapped-api-key-123";
            UUID mockUuid = UUID.randomUUID();
            when(mockEmailResponse.getNotificationId()).thenReturn(mockUuid);
            when(apiKeyMappingService.findMatchingApiKey(VALID_REFERENCE)).thenReturn(mappedApiKey);
            when(apiKeyMappingService.getNotificationClient(mappedApiKey)).thenReturn(mockMappedClient);
            when(mockMappedClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(mockEmailResponse);

            // When
            GovUkNotifyService.EmailResp result = govUkNotifyService.sendEmail(
                VALID_EMAIL, VALID_TEMPLATE_ID, VALID_REFERENCE, VALID_PERSONALISATION
            );

            // Then
            assertTrue(result.success());
            verify(apiKeyMappingService).findMatchingApiKey(VALID_REFERENCE);
            verify(apiKeyMappingService).getNotificationClient(mappedApiKey);
            verify(mockMappedClient).sendEmail(VALID_TEMPLATE_ID, VALID_EMAIL, VALID_PERSONALISATION, VALID_REFERENCE);
        }

        @Test
        @DisplayName("When_NoMappingMatches_Expect_DefaultClientUsedForLetter")
        void When_NoMappingMatches_Expect_DefaultClientUsedForLetter() throws NotificationClientException {
            // Given
            UUID mockUuid = UUID.randomUUID();
            when(mockLetterResponse.getNotificationId()).thenReturn(mockUuid);
            when(apiKeyMappingService.findMatchingApiKey(VALID_RECIPIENT)).thenReturn(null);
            when(mockClient.sendPrecompiledLetterWithInputStream(anyString(), any(InputStream.class)))
                .thenReturn(mockLetterResponse);

            // When
            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            // Then
            assertTrue(result.success());
            verify(apiKeyMappingService).findMatchingApiKey(VALID_RECIPIENT);
            verify(mockClient).sendPrecompiledLetterWithInputStream(VALID_RECIPIENT, mockPdf);
        }

        @Test
        @DisplayName("When_MappingMatches_Expect_MappedClientUsedForLetter")
        void When_MappingMatches_Expect_MappedClientUsedForLetter() throws NotificationClientException {
            // Given
            String mappedApiKey = "mapped-api-key-456";
            UUID mockUuid = UUID.randomUUID();
            when(mockLetterResponse.getNotificationId()).thenReturn(mockUuid);
            when(apiKeyMappingService.findMatchingApiKey(VALID_RECIPIENT)).thenReturn(mappedApiKey);
            when(apiKeyMappingService.getNotificationClient(mappedApiKey)).thenReturn(mockMappedClient);
            when(mockMappedClient.sendPrecompiledLetterWithInputStream(anyString(), any(InputStream.class)))
                .thenReturn(mockLetterResponse);

            // When
            GovUkNotifyService.LetterResp result = govUkNotifyService.sendLetter(VALID_RECIPIENT, mockPdf);

            // Then
            assertTrue(result.success());
            verify(apiKeyMappingService).findMatchingApiKey(VALID_RECIPIENT);
            verify(apiKeyMappingService).getNotificationClient(mappedApiKey);
            verify(mockMappedClient).sendPrecompiledLetterWithInputStream(VALID_RECIPIENT, mockPdf);
        }

        @Test
        @DisplayName("When_DifferentReferences_Expect_DifferentMappedClients")
        void When_DifferentReferences_Expect_DifferentMappedClients() throws NotificationClientException {
            // Given
            String reference1 = "e2e-test-1";
            String reference2 = "integration-test-1";
            String mappedKey1 = "api-key-1";
            String mappedKey2 = "api-key-2";

            NotificationClient client1 = mockMappedClient;
            NotificationClient client2 = org.mockito.Mockito.mock(NotificationClient.class);

            SendEmailResponse response1 = org.mockito.Mockito.mock(SendEmailResponse.class);
            SendEmailResponse response2 = org.mockito.Mockito.mock(SendEmailResponse.class);
            when(response1.getNotificationId()).thenReturn(UUID.randomUUID());
            when(response2.getNotificationId()).thenReturn(UUID.randomUUID());

            when(apiKeyMappingService.findMatchingApiKey(reference1)).thenReturn(mappedKey1);
            when(apiKeyMappingService.findMatchingApiKey(reference2)).thenReturn(mappedKey2);
            when(apiKeyMappingService.getNotificationClient(mappedKey1)).thenReturn(client1);
            when(apiKeyMappingService.getNotificationClient(mappedKey2)).thenReturn(client2);
            when(client1.sendEmail(anyString(), anyString(), anyMap(), eq(reference1))).thenReturn(response1);
            when(client2.sendEmail(anyString(), anyString(), anyMap(), eq(reference2))).thenReturn(response2);

            // When
            GovUkNotifyService.EmailResp result1 = govUkNotifyService.sendEmail(
                VALID_EMAIL, VALID_TEMPLATE_ID, reference1, VALID_PERSONALISATION
            );
            GovUkNotifyService.EmailResp result2 = govUkNotifyService.sendEmail(
                VALID_EMAIL, VALID_TEMPLATE_ID, reference2, VALID_PERSONALISATION
            );

            // Then
            assertTrue(result1.success());
            assertTrue(result2.success());
            verify(client1).sendEmail(VALID_TEMPLATE_ID, VALID_EMAIL, VALID_PERSONALISATION, reference1);
            verify(client2).sendEmail(VALID_TEMPLATE_ID, VALID_EMAIL, VALID_PERSONALISATION, reference2);
        }
    }
}
