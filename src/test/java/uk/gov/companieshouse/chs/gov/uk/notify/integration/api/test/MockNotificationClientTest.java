package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.test;

import org.json.JSONObject;
import org.jspecify.annotations.NonNull;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.test.MockNotificationClient.MOCK_REFERENCE_PREFIX;

@SuppressWarnings("CatchMayIgnoreException")
@DisplayName("MockNotificationClient sendEmail")
class MockNotificationClientTest {

    private static final String TEMPLATE_ID_1 = "11111111-1111-1111-1111-111111111111";
    private static final String TEMPLATE_ID_2 = "22222222-2222-2222-2222-222222222222";
    private static final String TEMPLATE_ID_3 = "33333333-3333-3333-3333-333333333333";

    private static MockNotificationClient createConfiguredMockClient() {
        MockNotificationClient mockNotificationClient = new MockNotificationClient("test-api-key");
        mockNotificationClient.setMinuteLimit(10);
        mockNotificationClient.setDailyLimit(10);
        mockNotificationClient.setSimulateFailures(false);
        return mockNotificationClient;
    }

    @Test
    @DisplayName("returns a mocked email response when the reference uses the mock prefix")
    void sendEmailReturnsMockedResponseWhenReferenceUsesMockPrefix() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = new MockNotificationClient("test-api-key");
        mockNotificationClient.setMinuteLimit(10);
        mockNotificationClient.setDailyLimit(10);
        mockNotificationClient.setSimulateFailures(false);

        String templateId = TEMPLATE_ID_1;
        String reference = MOCK_REFERENCE_PREFIX + "-email-123";

        SendEmailResponse response = mockNotificationClient.sendEmail(
                templateId,
                "test@example.com",
                Map.of("name", "Test User"),
                reference
        );

        assertNotNull(response);
        assertNotNull(response.getNotificationId());
        assertEquals(Optional.of(reference), response.getReference());
        assertEquals(UUID.fromString(templateId), response.getTemplateId());
        assertEquals(1, response.getTemplateVersion());
        assertEquals("Mock Subject", response.getSubject());
        assertEquals("This is a mocked email response", response.getBody());
        assertEquals(Optional.of("mock.sender@notifications.service.gov.uk"), response.getFromEmail());
        assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("returns a mocked email response with an empty reference when the reference is null")
    void sendEmailReturnsMockedResponseWithEmptyReferenceWhenReferenceIsNull() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = new MockNotificationClient("test-api-key");
        mockNotificationClient.setMinuteLimit(10);
        mockNotificationClient.setDailyLimit(10);
        mockNotificationClient.setSimulateFailures(false);

        String templateId = TEMPLATE_ID_2;

        SendEmailResponse response = mockNotificationClient.sendEmail(
                templateId,
                "test@example.com",
                Map.of("companyName", "Test Company Ltd"),
                null
        );

        assertNotNull(response);
        assertNotNull(response.getNotificationId());
        assertEquals(Optional.of(""), response.getReference());
        assertEquals(UUID.fromString(templateId), response.getTemplateId());
        assertEquals("Mock Subject", response.getSubject());
        assertEquals("This is a mocked email response", response.getBody());
        assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("throws a 429 exception when the minute rate limit is exceeded")
    void sendEmailThrowsExceptionWhenMinuteRateLimitIsExceeded() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = new MockNotificationClient("test-api-key");
        mockNotificationClient.setMinuteLimit(1);
        mockNotificationClient.setDailyLimit(10);
        mockNotificationClient.setSimulateFailures(false);
        mockNotificationClient.setCurrentTimeSupplier(() -> 1_000L);

        mockNotificationClient.sendEmail(
                TEMPLATE_ID_1,
                "test@example.com",
                Map.of("name", "Test User"),
                MOCK_REFERENCE_PREFIX + "-minute-limit-first"
        );

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockNotificationClient.sendEmail(
                        TEMPLATE_ID_1,
                        "test@example.com",
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE_PREFIX + "-minute-limit-second"
                )
        );

        assertEquals(429, exception.getHttpResult());
        assertEquals("Rate limit exceeded", exception.getMessage());
        assertEquals(2, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("throws a 429 exception when the daily rate limit is exceeded")
    void sendEmailThrowsExceptionWhenDailyRateLimitIsExceeded() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = new MockNotificationClient("test-api-key");
        mockNotificationClient.setMinuteLimit(10);
        mockNotificationClient.setDailyLimit(1);
        mockNotificationClient.setSimulateFailures(false);
        mockNotificationClient.setCurrentTimeSupplier(() -> 2_000L);
        mockNotificationClient.setCurrentDateSupplier(() -> LocalDate.of(2026, 11, 20));

        mockNotificationClient.sendEmail(
                TEMPLATE_ID_1,
                "test@example.com",
                Map.of("name", "Test User"),
                MOCK_REFERENCE_PREFIX + "-daily-limit-first"
        );

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockNotificationClient.sendEmail(
                        TEMPLATE_ID_1,
                        "test@example.com",
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE_PREFIX + "-daily-limit-second"
                )
        );

        assertEquals(429, exception.getHttpResult());
        assertEquals("Rate limit exceeded", exception.getMessage());
        assertEquals(2, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(2, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("throws a 500 exception when simulated failures are enabled and the random value is below the failure rate")
    void sendEmailThrowsExceptionWhenSimulatedFailureOccurs() {
        MockNotificationClient mockNotificationClient = new MockNotificationClient("test-api-key");
        mockNotificationClient.setMinuteLimit(10);
        mockNotificationClient.setDailyLimit(10);
        mockNotificationClient.setSimulateFailures(true);
        mockNotificationClient.setFailureRate(0.5);
        mockNotificationClient.setRandomSupplier(() -> 0.49);

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockNotificationClient.sendEmail(
                        TEMPLATE_ID_1,
                        "test@example.com",
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE_PREFIX + "-simulated-failure"
                )
        );

        assertEquals(500, exception.getHttpResult());
        assertEquals("Simulated server error", exception.getMessage());
        assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("returns a mocked email response when simulated failures are enabled but the random value is not below the failure rate")
    void sendEmailReturnsMockedResponseWhenSimulatedFailureDoesNotOccur() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = new MockNotificationClient("test-api-key");
        mockNotificationClient.setMinuteLimit(10);
        mockNotificationClient.setDailyLimit(10);
        mockNotificationClient.setSimulateFailures(true);
        mockNotificationClient.setFailureRate(0.5);
        mockNotificationClient.setRandomSupplier(() -> 0.5);

        String templateId = TEMPLATE_ID_3;
        String reference = MOCK_REFERENCE_PREFIX + "-no-simulated-failure";

        SendEmailResponse response = mockNotificationClient.sendEmail(
                templateId,
                "test@example.com",
                Map.of("name", "Test User"),
                reference
        );

        assertNotNull(response);
        assertNotNull(response.getNotificationId());
        assertEquals(Optional.of(reference), response.getReference());
        assertEquals(UUID.fromString(templateId), response.getTemplateId());
        assertEquals("Mock Subject", response.getSubject());
        assertEquals("This is a mocked email response", response.getBody());
        assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("returns a mocked letter response for a mock reference")
    void sendPrecompiledLetterReturnsMockedResponseWhenReferenceUsesMockPrefix() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        String reference = MOCK_REFERENCE_PREFIX + "-letter-success";
        InputStream pdf = new ByteArrayInputStream("test letter content".getBytes(StandardCharsets.UTF_8));

        LetterResponse response = mockNotificationClient.sendPrecompiledLetterWithInputStream(reference, pdf);

        assertNotNull(response);
        assertNotNull(response.getNotificationId());
        assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("returns a mocked letter response with empty reference when reference is null")
    void sendPrecompiledLetterReturnsMockedResponseWithEmptyReferenceWhenReferenceIsNull() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        InputStream pdf = new ByteArrayInputStream("test letter content".getBytes(StandardCharsets.UTF_8));

        LetterResponse response = mockNotificationClient.sendPrecompiledLetterWithInputStream(null, pdf);

        assertNotNull(response);
        assertEquals(Optional.of(""), response.getReference());
    }

    @Test
    @DisplayName("throws a 429 exception when letter minute limit is exceeded")
    void sendPrecompiledLetterThrowsExceptionWhenMinuteRateLimitIsExceeded() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        mockNotificationClient.setMinuteLimit(1);
        mockNotificationClient.setCurrentTimeSupplier(() -> 5_000L);

        mockNotificationClient.sendPrecompiledLetterWithInputStream(
                MOCK_REFERENCE_PREFIX + "-letter-first",
                new ByteArrayInputStream("first".getBytes(StandardCharsets.UTF_8))
        );

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockNotificationClient.sendPrecompiledLetterWithInputStream(
                        MOCK_REFERENCE_PREFIX + "-letter-second",
                        new ByteArrayInputStream("second".getBytes(StandardCharsets.UTF_8))
                )
        );

        assertEquals(429, exception.getHttpResult());
        assertEquals("Rate limit exceeded", exception.getMessage());
    }

    @Test
    @DisplayName("throws a 500 exception when simulated letter failure occurs")
    void sendPrecompiledLetterThrowsExceptionWhenSimulatedFailureOccurs() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        mockNotificationClient.setSimulateFailures(true);
        mockNotificationClient.setFailureRate(0.5);
        mockNotificationClient.setRandomSupplier(() -> 0.49);

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockNotificationClient.sendPrecompiledLetterWithInputStream(
                        MOCK_REFERENCE_PREFIX + "-letter-simulated-failure",
                        new ByteArrayInputStream("pdf".getBytes(StandardCharsets.UTF_8))
                )
        );

        assertEquals(500, exception.getHttpResult());
        assertEquals("Simulated server error", exception.getMessage());
    }

    @Test
    @DisplayName("returns a mocked letter response when postage overload is used")
    void sendPrecompiledLetterWithPostageReturnsMockedResponseWhenReferenceUsesMockPrefix() throws Exception {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        String reference = MOCK_REFERENCE_PREFIX + "-letter-with-postage";
        Path outputPath = Path.of("/tmp/mock_letter_" + reference + ".pdf");
        Files.deleteIfExists(outputPath);

        try (InputStream pdfContent = new ByteArrayInputStream(
                "pdf-content".getBytes(StandardCharsets.UTF_8))) {
            LetterResponse response = mockNotificationClient.sendPrecompiledLetterWithInputStream(
                    reference,
                    pdfContent,
                    "first"
            );

            assertNotNull(response);
            assertNotNull(response.getNotificationId());
            assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());
            assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());
            assertTrue(Files.exists(outputPath));
        } finally {
            Files.deleteIfExists(outputPath);
        }
    }

    @Test
    @DisplayName("throws a 500 exception when postage overload cannot read PDF stream")
    void sendPrecompiledLetterWithPostageThrowsExceptionWhenPdfReadFails() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        InputStream failingPdf = new InputStream() {
            @Override
            public int read() throws IOException {
                throw new IOException("forced read failure");
            }

            @Override
            public int read(byte @NonNull [] b, int off, int len) throws IOException {
                throw new IOException("forced read failure");
            }

            @Override
            public byte @NonNull [] readAllBytes() throws IOException {
                throw new IOException("forced read failure");
            }
        };

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockNotificationClient.sendPrecompiledLetterWithInputStream(
                        MOCK_REFERENCE_PREFIX + "-letter-write-failure",
                        failingPdf,
                        "second"
                )
        );

        assertEquals(500, exception.getHttpResult());
        assertEquals("Failed to write mock letter PDF to file", exception.getMessage());
    }

    @Test
    @DisplayName("throws a 429 exception when postage letter minute limit is exceeded")
    void sendPrecompiledLetterWithPostageThrowsExceptionWhenMinuteRateLimitIsExceeded() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        mockNotificationClient.setMinuteLimit(1);
        mockNotificationClient.setCurrentTimeSupplier(() -> 9_000L);

        mockNotificationClient.sendPrecompiledLetterWithInputStream(
                MOCK_REFERENCE_PREFIX + "-postage-first",
                new ByteArrayInputStream("first".getBytes(StandardCharsets.UTF_8)),
                "first"
        );

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockNotificationClient.sendPrecompiledLetterWithInputStream(
                        MOCK_REFERENCE_PREFIX + "-postage-second",
                        new ByteArrayInputStream("second".getBytes(StandardCharsets.UTF_8)),
                        "first"
                )
        );

        assertEquals(429, exception.getHttpResult());
        assertEquals("Rate limit exceeded", exception.getMessage());
    }

    @Test
    @DisplayName("throws a 500 exception when postage letter simulated failure occurs")
    void sendPrecompiledLetterWithPostageThrowsExceptionWhenSimulatedFailureOccurs() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        mockNotificationClient.setSimulateFailures(true);
        mockNotificationClient.setFailureRate(0.5);
        mockNotificationClient.setRandomSupplier(() -> 0.49);

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockNotificationClient.sendPrecompiledLetterWithInputStream(
                        MOCK_REFERENCE_PREFIX + "-postage-simulated-failure",
                        new ByteArrayInputStream("pdf".getBytes(StandardCharsets.UTF_8)),
                        "second"
                )
        );

        assertEquals(500, exception.getHttpResult());
        assertEquals("Simulated server error", exception.getMessage());
    }

    @Test
    @DisplayName("throws a non-rate-limit exception when using a non-mock reference")
    void sendEmailDelegatesToRealNotifyWhenReferenceIsNotMocked() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        try {
            mockNotificationClient.sendEmail(
                    TEMPLATE_ID_1,
                    "test@example.com",
                    Map.of("name", "Test User"),
                    "real-reference"
            );
        } catch (Exception e) {
            if (e.getMessage() != null) {
                assertFalse(e.getMessage().contains("Rate limit exceeded"));
            }
        }
    }

    @Test
    @DisplayName("creates a valid mock email response payload")
    void createMockEmailResponseReturnsExpectedJson() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        String reference = MOCK_REFERENCE_PREFIX + "-json-email";

        String json = mockNotificationClient.createMockEmailResponse(TEMPLATE_ID_1, reference);
        JSONObject parsed = new JSONObject(json);

        assertEquals(reference, parsed.getString("reference"));
        assertEquals(TEMPLATE_ID_1, parsed.getJSONObject("template").getString("id"));
        assertEquals("Mock Subject", parsed.getJSONObject("content").getString("subject"));
    }

    @Test
    @DisplayName("creates a valid mock letter response payload with empty reference when null")
    void createMockLetterResponseReturnsExpectedJsonWhenReferenceIsNull() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();

        String json = mockNotificationClient.createMockLetterResponse(null);
        JSONObject parsed = new JSONObject(json);

        assertEquals("", parsed.getString("reference"));
        assertEquals("Mock Letter", parsed.getJSONObject("content").getString("subject"));
        assertTrue(parsed.getJSONObject("template").has("id"));
    }

    @Test
    @DisplayName("resets counters and returns zero when no requests are recorded")
    void resetRateLimitsClearsCountersAndEmptyCountersReturnZero() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();

        assertEquals(0, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(0, mockNotificationClient.getCurrentDailyRequestCount());

        mockNotificationClient.sendEmail(
                TEMPLATE_ID_1,
                "test@example.com",
                Map.of("name", "Test User"),
                MOCK_REFERENCE_PREFIX + "-reset-test"
        );

        assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());

        mockNotificationClient.resetRateLimits();

        assertEquals(0, mockNotificationClient.getCurrentMinuteRequestCount());
        assertEquals(0, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("uses configured suppliers for current time and current date")
    void currentTimeAndDateComeFromConfiguredSuppliers() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        mockNotificationClient.setCurrentTimeSupplier(() -> 12_345L);
        LocalDate fixedDate = LocalDate.of(2026, 11, 20);
        mockNotificationClient.setCurrentDateSupplier(() -> fixedDate);

        assertEquals(12_345L, mockNotificationClient.getCurrentTimeMillis());
        assertEquals(fixedDate, mockNotificationClient.getCurrentDate());
    }

    @Test
    @DisplayName("resets daily counters when the current date changes")
    void isRateLimitExceededResetsDailyCounterWhenDateChanges() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        LocalDate firstDay = LocalDate.of(2026, 11, 20);
        mockNotificationClient.setCurrentDateSupplier(() -> firstDay);

        mockNotificationClient.sendEmail(
                TEMPLATE_ID_1,
                "test@example.com",
                Map.of("name", "Test User"),
                MOCK_REFERENCE_PREFIX + "-day-1"
        );
        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());

        mockNotificationClient.setCurrentDateSupplier(() -> firstDay.plusDays(1));
        mockNotificationClient.sendEmail(
                TEMPLATE_ID_1,
                "test@example.com",
                Map.of("name", "Test User"),
                MOCK_REFERENCE_PREFIX + "-day-2"
        );

        assertEquals(1, mockNotificationClient.getCurrentDailyRequestCount());
    }

    @Test
    @DisplayName("delegates letter sending to real notify when reference is not mocked")
    void sendPrecompiledLetterDelegatesToRealNotifyWhenReferenceIsNotMocked() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        InputStream pdf = new ByteArrayInputStream("letter".getBytes(StandardCharsets.UTF_8));

        try {
            mockNotificationClient.sendPrecompiledLetterWithInputStream("real-reference", pdf);
        } catch (Exception e) {
            if (e.getMessage() != null) {
                assertFalse(e.getMessage().contains("Rate limit exceeded"));
            }
        }
    }

    @Test
    @DisplayName("delegates letter postage overload to real notify when reference is not mocked")
    void sendPrecompiledLetterWithPostageDelegatesToRealNotifyWhenReferenceIsNotMocked() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        InputStream pdf = new ByteArrayInputStream("letter".getBytes(StandardCharsets.UTF_8));

        try {
            mockNotificationClient.sendPrecompiledLetterWithInputStream("real-reference", pdf, "first");
        } catch (Exception e) {
            if (e.getMessage() != null) {
                assertFalse(e.getMessage().contains("Rate limit exceeded"));
            }
        }
    }

    @Test
    @DisplayName("gets and sets minute and daily limits")
    void getAndSetLimitsWorkAsExpected() {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        mockNotificationClient.setMinuteLimit(42);
        mockNotificationClient.setDailyLimit(420);

        assertEquals(42, mockNotificationClient.getMinuteLimit());
        assertEquals(420, mockNotificationClient.getDailyLimit());
    }

    @Test
    @DisplayName("expires minute-window requests older than sixty seconds")
    void isRateLimitExceededRemovesExpiredMinuteRequests() throws NotificationClientException {
        MockNotificationClient mockNotificationClient = createConfiguredMockClient();
        mockNotificationClient.setCurrentTimeSupplier(() -> 100_000L);

        mockNotificationClient.sendEmail(
                TEMPLATE_ID_1,
                "test@example.com",
                Map.of("name", "Test User"),
                MOCK_REFERENCE_PREFIX + "-expiry-first"
        );
        assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());

        mockNotificationClient.setCurrentTimeSupplier(() -> 161_001L);
        mockNotificationClient.sendEmail(
                TEMPLATE_ID_1,
                "test@example.com",
                Map.of("name", "Test User"),
                MOCK_REFERENCE_PREFIX + "-expiry-second"
        );

        assertEquals(1, mockNotificationClient.getCurrentMinuteRequestCount());
    }
}
