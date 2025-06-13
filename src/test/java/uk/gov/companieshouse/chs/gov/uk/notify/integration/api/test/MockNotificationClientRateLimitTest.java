package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.test;

import java.io.ByteArrayInputStream;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(properties = {"spring.data.mongodb.uri=mongodb://token_value"})
@ActiveProfiles("test")
class MockNotificationClientRateLimitTest {

    @Autowired
    private MockNotificationClient mockClient;

    private static final String MOCK_REFERENCE = MockNotificationClient.MOCK_REFERENCE_PREFIX + "-test";
    private static final String TEST_TEMPLATE_ID = UUID.randomUUID().toString();
    private static final String TEST_EMAIL = "test@example.com";
    private static final InputStream TEST_PDF = new ByteArrayInputStream("test content".getBytes());

    @BeforeEach
    public void setup() {
        mockClient.resetRateLimits();
        mockClient.setCurrentTimeSupplier(System::currentTimeMillis);
        mockClient.setCurrentDateSupplier(LocalDate::now);
        mockClient.setMinuteLimit(5);
        mockClient.setDailyLimit(10);
        mockClient.setSimulateFailures(false);
    }

    @Test
    void When_MinuteLimitReached_Expect_RateLimitExceptionAndResetAfterOneMinute() throws NotificationClientException {
        final long baseTime = System.currentTimeMillis();
        final AtomicLong currentTime = new AtomicLong(baseTime);
        mockClient.setCurrentTimeSupplier(currentTime::get);

        for (int i = 0; i < mockClient.getMinuteLimit(); i++) {
            SendEmailResponse response = mockClient.sendEmail(
                    TEST_TEMPLATE_ID,
                    TEST_EMAIL,
                    Map.of("name", "Test User"),
                    MOCK_REFERENCE
            );
            assertNotNull(response);
            assertNotNull(response.getNotificationId());
            assertEquals(i + 1, mockClient.getCurrentMinuteRequestCount());
        }

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockClient.sendEmail(
                        TEST_TEMPLATE_ID,
                        TEST_EMAIL,
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE
                )
        );

        assertEquals(429, exception.getHttpResult());
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));

        currentTime.set(baseTime + 61000);
        SendEmailResponse response = mockClient.sendEmail(
                TEST_TEMPLATE_ID,
                TEST_EMAIL,
                Map.of("name", "Test User"),
                MOCK_REFERENCE
        );

        assertNotNull(response);
        assertEquals(1, mockClient.getCurrentMinuteRequestCount());
    }

    @Test
    void When_DailyLimitReached_Expect_RateLimitExceptionAndResetNextDay() throws NotificationClientException {
        final LocalDate baseDate = LocalDate.now();
        mockClient.setCurrentDateSupplier(() -> baseDate);

        for (int i = 0; i < mockClient.getDailyLimit(); i++) {
            final int currentIndex = i;
            mockClient.setCurrentTimeSupplier(() -> System.currentTimeMillis() + currentIndex * 61000L);

            SendEmailResponse response = mockClient.sendEmail(
                    TEST_TEMPLATE_ID,
                    TEST_EMAIL,
                    Map.of("name", "Test User"),
                    MOCK_REFERENCE
            );
            assertNotNull(response);

            assertEquals(i + 1, mockClient.getCurrentDailyRequestCount());
        }

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockClient.sendEmail(
                        TEST_TEMPLATE_ID,
                        TEST_EMAIL,
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE
                )
        );

        assertEquals(429, exception.getHttpResult());
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));

        mockClient.setCurrentDateSupplier(() -> baseDate.plusDays(1));

        SendEmailResponse response = mockClient.sendEmail(
                TEST_TEMPLATE_ID,
                TEST_EMAIL,
                Map.of("name", "Test User"),
                MOCK_REFERENCE
        );

        assertNotNull(response);
        assertEquals(1, mockClient.getCurrentDailyRequestCount());
    }

    @Test
    void When_LetterMinuteLimitReached_Expect_RateLimitExceptionAndResetAfterOneMinute() throws NotificationClientException {
        final long baseTime = System.currentTimeMillis();
        final AtomicLong currentTime = new AtomicLong(baseTime);
        mockClient.setCurrentTimeSupplier(currentTime::get);

        for (int i = 0; i < mockClient.getMinuteLimit(); i++) {
            LetterResponse response = mockClient.sendPrecompiledLetterWithInputStream(
                    MOCK_REFERENCE + "-" + i,
                    TEST_PDF
            );
            assertNotNull(response);
            assertNotNull(response.getNotificationId());

            assertEquals(i + 1, mockClient.getCurrentMinuteRequestCount());
        }

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockClient.sendPrecompiledLetterWithInputStream(
                        MOCK_REFERENCE + "-limit-exceeded",
                        TEST_PDF
                )
        );

        assertEquals(429, exception.getHttpResult());
        assertTrue(exception.getMessage().contains("Rate limit exceeded"));
        currentTime.set(baseTime + 61000);

        LetterResponse response = mockClient.sendPrecompiledLetterWithInputStream(
                MOCK_REFERENCE + "-after-reset",
                TEST_PDF
        );

        assertNotNull(response);
        assertEquals(1, mockClient.getCurrentMinuteRequestCount());
    }

    @Test
    void When_CombinedEmailAndLetterLimitReached_Expect_RateLimitException() throws NotificationClientException {
        final long baseTime = System.currentTimeMillis();
        final AtomicLong currentTime = new AtomicLong(baseTime);
        mockClient.setCurrentTimeSupplier(currentTime::get);
        mockClient.setMinuteLimit(4);

        for (int i = 0; i < mockClient.getMinuteLimit() / 2; i++) {
            SendEmailResponse response = mockClient.sendEmail(
                    TEST_TEMPLATE_ID,
                    TEST_EMAIL,
                    Map.of("name", "Test User"),
                    MOCK_REFERENCE + "-email-" + i
            );
            assertNotNull(response);
        }

        for (int i = 0; i < mockClient.getMinuteLimit() / 2; i++) {
            LetterResponse response = mockClient.sendPrecompiledLetterWithInputStream(
                    MOCK_REFERENCE + "-letter-" + i,
                    TEST_PDF
            );
            assertNotNull(response);
        }

        assertEquals(mockClient.getMinuteLimit(), mockClient.getCurrentMinuteRequestCount());

        assertThrows(
                NotificationClientException.class,
                () -> mockClient.sendEmail(
                        TEST_TEMPLATE_ID,
                        TEST_EMAIL,
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE + "-over-limit"
                )
        );

        assertThrows(
                NotificationClientException.class,
                () -> mockClient.sendPrecompiledLetterWithInputStream(
                        MOCK_REFERENCE + "-over-limit",
                        TEST_PDF
                )
        );
    }

    @Test
    void When_FailureSimulationEnabled_Expect_SimulatedServerError() throws NotificationClientException {
        mockClient.setSimulateFailures(true);
        mockClient.setFailureRate(1.0);

        NotificationClientException exception = assertThrows(
                NotificationClientException.class,
                () -> mockClient.sendEmail(
                        TEST_TEMPLATE_ID,
                        TEST_EMAIL,
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE
                )
        );

        assertEquals(500, exception.getHttpResult());
        assertTrue(exception.getMessage().contains("Simulated server error"));

        mockClient.setSimulateFailures(false);

        SendEmailResponse response = mockClient.sendEmail(
                TEST_TEMPLATE_ID,
                TEST_EMAIL,
                Map.of("name", "Test User"),
                MOCK_REFERENCE
        );

        assertNotNull(response);
    }

    @Test
    void When_NonMockReferenceUsed_Expect_RateLimitsBypass() throws NotificationClientException {
        mockClient.setMinuteLimit(1);
        mockClient.setDailyLimit(1);

        SendEmailResponse mockResponse = mockClient.sendEmail(
                TEST_TEMPLATE_ID,
                TEST_EMAIL,
                Map.of("name", "Test User"),
                MOCK_REFERENCE
        );
        assertNotNull(mockResponse);

        assertThrows(
                NotificationClientException.class,
                () -> mockClient.sendEmail(
                        TEST_TEMPLATE_ID,
                        TEST_EMAIL,
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE
                )
        );

        // But a real reference should bypass the mock functionality entirely
        // This will only work if there's a real API key, but in tests it would still
        // reach a different point in the code (the super class call) before failing
        try {
            mockClient.sendEmail(
                    TEST_TEMPLATE_ID,
                    TEST_EMAIL,
                    Map.of("name", "Test User"),
                    "regular-reference-not-mocked"
            );
            // If we get here, either:
            // 1. We have a valid API key, and it worked, or
            // 2. The mock client's super call was intercepted by another mock in the test context
            // Either way, we bypassed our rate limit check which is what we're testing
        } catch (Exception e) {
            // Expected in most test environments due to invalid/missing API key
            // We just need to verify it wasn't a rate limit exception
            assertFalse(e.getMessage().contains("Rate limit exceeded"));
        }
    }

    @Test
    void When_PartialFailureRateSet_Expect_MixOfSuccessesAndFailures() {
        mockClient.setMinuteLimit(100);
        mockClient.setDailyLimit(100);

        double[] randomValues = new double[20];
        for (int i = 0; i < 20; i++) {
            randomValues[i] = (i % 2 == 0) ? 0.0 : 1.0;
        }

        AtomicInteger randomIndex = new AtomicInteger(0);
        mockClient.setRandomSupplier(() -> {
            int index = randomIndex.getAndIncrement() % randomValues.length;
            return randomValues[index];
        });

        mockClient.setSimulateFailures(true);
        mockClient.setFailureRate(0.5); // Failures when random value >= 0.5

        int successCount = 0;
        int failureCount = 0;

        for (int i = 0; i < 20; i++) {
            try {
                SendEmailResponse response = mockClient.sendEmail(
                        TEST_TEMPLATE_ID,
                        TEST_EMAIL,
                        Map.of("name", "Test User"),
                        MOCK_REFERENCE + "-" + i
                );
                assertNotNull(response);
                successCount++;
            } catch (NotificationClientException e) {
                assertEquals(500, e.getHttpResult());
                assertTrue(e.getMessage().contains("Simulated server error"));
                failureCount++;
            }
        }

        assertEquals(10, successCount, "Should have exactly 10 successful requests");
        assertEquals(10, failureCount, "Should have exactly 10 failed requests");
    }

    @Test
    void When_RequestsAreBelowLimit_Expect_AllSucceed() throws NotificationClientException {
        mockClient.setMinuteLimit(5);

        for (int i = 0; i < mockClient.getMinuteLimit() - 1; i++) {
            SendEmailResponse response = mockClient.sendEmail(
                    TEST_TEMPLATE_ID,
                    TEST_EMAIL,
                    Map.of("name", "Test User"),
                    MOCK_REFERENCE + "-" + i
            );
            assertNotNull(response);
            assertEquals(i + 1, mockClient.getCurrentMinuteRequestCount());
        }

        assertEquals(mockClient.getMinuteLimit() - 1, mockClient.getCurrentMinuteRequestCount());

        SendEmailResponse response = mockClient.sendEmail(
                TEST_TEMPLATE_ID,
                TEST_EMAIL,
                Map.of("name", "Test User"),
                MOCK_REFERENCE + "-final"
        );

        assertNotNull(response);
        assertEquals(mockClient.getMinuteLimit(), mockClient.getCurrentMinuteRequestCount());
    }

    @Test
    void When_TimeRollsForwardPartially_Expect_OldRequestsExpireFromMinuteCounter() throws NotificationClientException {
        final long baseTime = System.currentTimeMillis();
        final AtomicLong currentTime = new AtomicLong(baseTime);
        mockClient.setCurrentTimeSupplier(currentTime::get);
        mockClient.setMinuteLimit(5);

        for (int i = 0; i < 3; i++) {
            mockClient.sendEmail(
                    TEST_TEMPLATE_ID,
                    TEST_EMAIL,
                    Map.of("name", "Test User"),
                    MOCK_REFERENCE + "-initial-" + i
            );
        }

        currentTime.set(baseTime + 30000);

        assertEquals(3, mockClient.getCurrentMinuteRequestCount());

        currentTime.set(baseTime + 65000);

        mockClient.sendEmail(
                TEST_TEMPLATE_ID,
                TEST_EMAIL,
                Map.of("name", "Test User"),
                MOCK_REFERENCE + "-after-expiry"
        );

        assertEquals(1, mockClient.getCurrentMinuteRequestCount());
    }

    @Test
    void When_DateChanges_Expect_DailyCounterReset() throws NotificationClientException {
        final LocalDate baseDate = LocalDate.now();
        mockClient.setCurrentDateSupplier(() -> baseDate);

        for (int i = 0; i < 3; i++) {
            mockClient.sendEmail(
                    TEST_TEMPLATE_ID,
                    TEST_EMAIL,
                    Map.of("name", "Test User"),
                    MOCK_REFERENCE + "-day1-" + i
            );
        }

        assertEquals(3, mockClient.getCurrentDailyRequestCount());

        mockClient.setCurrentDateSupplier(() -> baseDate.plusDays(1));
        mockClient.sendEmail(
                TEST_TEMPLATE_ID,
                TEST_EMAIL,
                Map.of("name", "Test User"),
                MOCK_REFERENCE + "-day2"
        );

        assertEquals(1, mockClient.getCurrentDailyRequestCount());
    }
}
