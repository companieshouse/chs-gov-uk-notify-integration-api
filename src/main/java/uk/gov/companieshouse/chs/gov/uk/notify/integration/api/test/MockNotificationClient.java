package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.test;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import java.io.InputStream;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Primary;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;
import uk.gov.service.notify.LetterResponse;
import uk.gov.service.notify.NotificationClient;
import uk.gov.service.notify.NotificationClientException;
import uk.gov.service.notify.SendEmailResponse;

@Component
@Primary
@Profile({"local", "dev", "test"}) // Only active in non-production environments
public class MockNotificationClient extends NotificationClient {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);

    public static final String MOCK_REFERENCE_PREFIX = "use-mock-notify";

    private static final String RATE_LIMIT_EXCEEDED = "Rate limit exceeded";
    private static final String SIMULATED_SERVER_ERROR = "Simulated server error";
    private static final String DEFAULT_KEY = "default";

    private final Map<String, List<Long>> minuteRequests = new ConcurrentHashMap<>();
    private final Map<String, AtomicInteger> dailyRateLimits = new ConcurrentHashMap<>();
    private LocalDate lastDayReset = LocalDate.now();
    private Supplier<Double> randomSupplier = Math::random;

    @Value("${notify.mock.minute-limit:3000}")
    private int minuteLimit;

    @Value("${notify.mock.daily-limit:250000}")
    private int dailyLimit;

    @Value("${notify.mock.simulate-failures:false}")
    private boolean simulateFailures;

    @Value("${notify.mock.failure-rate:0.1}")
    private double failureRate;

    private Supplier<Long> currentTimeSupplier = System::currentTimeMillis;
    private Supplier<LocalDate> currentDateSupplier = LocalDate::now;

    private static class NotificationClientExceptionMock extends NotificationClientException {
        private final int httpResult;

        public NotificationClientExceptionMock(String message, int httpResult) {
            super(message);
            this.httpResult = httpResult;
        }

        @Override
        public int getHttpResult() {
            return this.httpResult;
        }
    }

    public MockNotificationClient(@Value("${gov.uk.notify.api.key}") String apiKey) {
        super(apiKey);
        LOGGER.info("Using mock notification client, must be test environment.");
    }

    private boolean useRealGovNotify(String reference) {
        return reference != null && !reference.startsWith(MOCK_REFERENCE_PREFIX);
    }

    @Override
    public SendEmailResponse sendEmail(String templateId, String emailAddress, Map<String, ?> personalisation,
                                       String reference) throws NotificationClientException {
        if (useRealGovNotify(reference)) {
            return super.sendEmail(templateId, emailAddress, personalisation, reference);
        }

        if (isRateLimitExceeded()) {
            throw new NotificationClientExceptionMock(RATE_LIMIT_EXCEEDED, 429);
        }

        if (shouldEmulateChanceFailure()) {
            throw new NotificationClientExceptionMock(SIMULATED_SERVER_ERROR, 500);
        }

        String jsonResponse = createMockEmailResponse(templateId, reference);
        return new SendEmailResponse(jsonResponse);
    }

    private boolean shouldEmulateChanceFailure() {
        return simulateFailures && randomSupplier.get() < failureRate;
    }

    @Override
    public LetterResponse sendPrecompiledLetterWithInputStream(String reference,
                                                               InputStream precompiledPdf)
            throws NotificationClientException {
        if (useRealGovNotify(reference)) {
            return super.sendPrecompiledLetterWithInputStream(reference, precompiledPdf);
        }

        if (isRateLimitExceeded()) {
            throw new NotificationClientExceptionMock(RATE_LIMIT_EXCEEDED, 429);
        }

        if (shouldEmulateChanceFailure()) {
            throw new NotificationClientExceptionMock(SIMULATED_SERVER_ERROR, 500);
        }

        String jsonResponse = createMockLetterResponse(reference);
        return new LetterResponse(jsonResponse);
    }

    public boolean isRateLimitExceeded() {
        long currentTime = getCurrentTimeMillis();

        long cutoffTime = currentTime - 60000;
        minuteRequests.computeIfPresent(DEFAULT_KEY, (k, timestamps) -> {
            timestamps.removeIf(timestamp -> timestamp < cutoffTime);
            return timestamps;
        });

        minuteRequests.putIfAbsent(DEFAULT_KEY, new CopyOnWriteArrayList<>());
        List<Long> timestamps = minuteRequests.get(DEFAULT_KEY);
        timestamps.add(currentTime);

        if (timestamps.size() > minuteLimit) {
            return true;
        }

        LocalDate today = getCurrentDate();
        if (!today.equals(lastDayReset)) {
            dailyRateLimits.clear();
            lastDayReset = today;
        }

        dailyRateLimits.putIfAbsent(DEFAULT_KEY, new AtomicInteger(0));
        int dailyCount = dailyRateLimits.get(DEFAULT_KEY).incrementAndGet();
        return dailyCount > dailyLimit;
    }

    public String createMockEmailResponse(String templateId, String reference) {
        var notificationId = UUID.randomUUID().toString();

        JSONObject content = new JSONObject()
                .put("subject", "Mock Subject")
                .put("body", "This is a mocked email response")
                .put("from_email", "mock.sender@notifications.service.gov.uk");

        JSONObject template = new JSONObject()
                .put("id", templateId)
                .put("version", 1)
                .put("uri", "https://api.notifications.service.gov.uk/v2/template/" + templateId);

        JSONObject response = new JSONObject()
                .put("id", notificationId)
                .put("reference", reference != null ? reference : "")
                .put("content", content)
                .put("template", template);

        return response.toString();
    }

    public String createMockLetterResponse(String reference) {
        var notificationId = UUID.randomUUID().toString();
        var templateId = UUID.randomUUID().toString();

        JSONObject content = new JSONObject()
                .put("subject", "Mock Letter")
                .put("body", "This is a mocked letter response");

        JSONObject template = new JSONObject()
                .put("id", templateId)
                .put("version", 1)
                .put("uri", "https://api.notifications.service.gov.uk/v2/template/" + templateId);

        JSONObject response = new JSONObject()
                .put("id", notificationId)
                .put("reference", reference != null ? reference : "")
                .put("content", content)
                .put("scheduled_for", JSONObject.NULL)
                .put("template", template);

        return response.toString();
    }

    public void resetRateLimits() {
        minuteRequests.clear();
        dailyRateLimits.clear();
    }

    public int getMinuteLimit() {
        return minuteLimit;
    }

    public void setMinuteLimit(int minuteLimit) {
        this.minuteLimit = minuteLimit;
    }

    public int getDailyLimit() {
        return dailyLimit;
    }

    public void setDailyLimit(int dailyLimit) {
        this.dailyLimit = dailyLimit;
    }

    public void setSimulateFailures(boolean simulateFailures) {
        this.simulateFailures = simulateFailures;
    }

    public void setFailureRate(double failureRate) {
        this.failureRate = failureRate;
    }

    protected long getCurrentTimeMillis() {
        return currentTimeSupplier.get();
    }

    protected LocalDate getCurrentDate() {
        return currentDateSupplier.get();
    }

    public void setCurrentTimeSupplier(Supplier<Long> currentTimeSupplier) {
        this.currentTimeSupplier = currentTimeSupplier;
    }

    public void setCurrentDateSupplier(Supplier<LocalDate> currentDateSupplier) {
        this.currentDateSupplier = currentDateSupplier;
    }

    public int getCurrentMinuteRequestCount() {
        if (!minuteRequests.containsKey(DEFAULT_KEY)) {
            return 0;
        }
        return minuteRequests.get(DEFAULT_KEY).size();
    }

    public int getCurrentDailyRequestCount() {
        if (!dailyRateLimits.containsKey(DEFAULT_KEY)) {
            return 0;
        }
        return dailyRateLimits.get(DEFAULT_KEY).get();
    }

    public void setRandomSupplier(Supplier<Double> randomSupplier) {
        this.randomSupplier = randomSupplier;
    }
} 
