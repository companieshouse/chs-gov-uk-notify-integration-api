package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.emailfacade;

import java.util.Map;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
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
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@Tag("unit-test")
public class GovUKNotifyEmailFacadeTest {

    private GovUKNotifyEmailFacade govUKNotifyEmailFacade;

    @Mock
    private NotificationClient mockClient;

    @Mock
    private SendEmailResponse mockResponse;

    private static final String VALID_EMAIL = "test@example.com";
    private static final String VALID_TEMPLATE_ID = "valid-template-id";
    private static final Map<String, String> VALID_PERSONALISATION = Map.of("name", "Test User");

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);

        ProxyFactory factory = new ProxyFactory(new GovUKNotifyEmailFacade("test-api-key"));
        Validator validator = Validation.buildDefaultValidatorFactory().getValidator();
        factory.addAdvice(new MethodValidationInterceptor(validator));
        govUKNotifyEmailFacade = (GovUKNotifyEmailFacade) factory.getProxy();

        ReflectionTestUtils.setField(govUKNotifyEmailFacade, "client", mockClient);
    }

    @Nested
    @DisplayName("Synchronous Email Sending Tests")
    class SynchronousEmailTests {

        @Test
        @DisplayName("When_ValidEmailAndTemplateProvided_Expect_SendEmailSucceeds")
        void When_ValidEmailAndTemplateProvided_Expect_SendEmailSucceeds() throws NotificationClientException {
            UUID mockUuid = UUID.randomUUID();
            when(mockResponse.getNotificationId()).thenReturn(mockUuid);
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(mockResponse);

            boolean result = govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);

            assertTrue(result);
            verify(mockClient).sendEmail(eq(VALID_TEMPLATE_ID), eq(VALID_EMAIL), eq(VALID_PERSONALISATION), anyString());
        }

        @Test
        @DisplayName("When_ClientReturnsNullResponse_Expect_SendEmailReturnsFalse")
        void When_ClientReturnsNullResponse_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(null);

            boolean result = govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);

            assertFalse(result);
        }

        @Test
        @DisplayName("When_ResponseHasNullNotificationId_Expect_SendEmailReturnsFalse")
        void When_ResponseHasNullNotificationId_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockResponse.getNotificationId()).thenReturn(null);
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(mockResponse);

            boolean result = govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);

            assertFalse(result);
        }

        @Test
        @DisplayName("When_ClientThrowsException_Expect_SendEmailReturnsFalse")
        void When_ClientThrowsException_Expect_SendEmailReturnsFalse() throws NotificationClientException {
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
                    .thenThrow(new NotificationClientException("Test exception"));

            boolean result = govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);

            assertFalse(result);
        }

        @Test
        @DisplayName("When_NullPersonalisationProvided_Expect_SendEmailSucceeds")
        void When_NullPersonalisationProvided_Expect_SendEmailSucceeds() throws NotificationClientException {
            UUID mockUuid = UUID.randomUUID();
            when(mockResponse.getNotificationId()).thenReturn(mockUuid);
            when(mockClient.sendEmail(anyString(), anyString(), isNull(), anyString())).thenReturn(mockResponse);

            boolean result = govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, VALID_TEMPLATE_ID, null);

            assertTrue(result);
            verify(mockClient).sendEmail(eq(VALID_TEMPLATE_ID), eq(VALID_EMAIL), isNull(), anyString());
        }
    }

    @Nested
    @DisplayName("Asynchronous Email Sending Tests")
    class AsynchronousEmailTests {

        @Test
        @DisplayName("When_ValidEmailAndTemplateProvided_Expect_SendEmailAsyncSucceeds")
        void When_ValidEmailAndTemplateProvided_Expect_SendEmailAsyncSucceeds() throws NotificationClientException, ExecutionException, InterruptedException {
            UUID mockUuid = UUID.randomUUID();
            when(mockResponse.getNotificationId()).thenReturn(mockUuid);
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString())).thenReturn(mockResponse);

            CompletableFuture<Boolean> future = govUKNotifyEmailFacade.sendEmailAsync(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);
            boolean result = future.get();

            assertTrue(result);
            verify(mockClient).sendEmail(eq(VALID_TEMPLATE_ID), eq(VALID_EMAIL), eq(VALID_PERSONALISATION), anyString());
        }

        @Test
        @DisplayName("When_ClientThrowsException_Expect_SendEmailAsyncReturnsFalse")
        void When_ClientThrowsException_Expect_SendEmailAsyncReturnsFalse() throws NotificationClientException, ExecutionException, InterruptedException {
            when(mockClient.sendEmail(anyString(), anyString(), anyMap(), anyString()))
                    .thenThrow(new NotificationClientException("Test exception"));

            CompletableFuture<Boolean> future = govUKNotifyEmailFacade.sendEmailAsync(VALID_EMAIL, VALID_TEMPLATE_ID, VALID_PERSONALISATION);
            boolean result = future.get();

            assertFalse(result);
        }
    }

    @Nested
    @DisplayName("Input Validation Tests")
    class ValidationTests {

        @DisplayName("When_InvalidEmailProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "When invalid email: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"invalid-email", "missing-at.com", "@missinguser.com"})
        void When_InvalidEmailProvided_Expect_ConstraintViolationException(String invalidEmail) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUKNotifyEmailFacade.sendEmail(invalidEmail, VALID_TEMPLATE_ID, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_InvalidTemplateIdProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "When invalid template ID: {0}")
        @NullAndEmptySource
        void When_InvalidTemplateIdProvided_Expect_ConstraintViolationException(String invalidTemplateId) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUKNotifyEmailFacade.sendEmail(VALID_EMAIL, invalidTemplateId, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_InvalidEmailProvidedForAsync_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "When invalid email for async: {0}")
        @NullAndEmptySource
        @ValueSource(strings = {"invalid-email", "missing-at.com"})
        void When_InvalidEmailProvidedForAsync_Expect_ConstraintViolationException(String invalidEmail) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUKNotifyEmailFacade.sendEmailAsync(invalidEmail, VALID_TEMPLATE_ID, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_InvalidTemplateIdProvidedForAsync_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "When invalid template ID for async: {0}")
        @NullAndEmptySource
        void When_InvalidTemplateIdProvidedForAsync_Expect_ConstraintViolationException(String invalidTemplateId) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUKNotifyEmailFacade.sendEmailAsync(VALID_EMAIL, invalidTemplateId, VALID_PERSONALISATION)
            );
        }

        @DisplayName("When_MultipleInvalidInputsProvided_Expect_ConstraintViolationException")
        @ParameterizedTest(name = "Email: {0}, TemplateId: {1}")
        @MethodSource("provideInvalidInputCombinations")
        void When_MultipleInvalidInputsProvided_Expect_ConstraintViolationException(
                String email, String templateId, String testDescription) {
            assertThrows(ConstraintViolationException.class, () ->
                    govUKNotifyEmailFacade.sendEmail(email, templateId, VALID_PERSONALISATION)
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
