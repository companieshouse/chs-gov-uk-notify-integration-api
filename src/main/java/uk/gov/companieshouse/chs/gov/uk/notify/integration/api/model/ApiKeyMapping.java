package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

import java.time.LocalDateTime;
import java.util.UUID;
import java.util.regex.Pattern;

/**
 * Represents an API key mapping for testing purposes.
 *
 * <p><strong>WARNING: FOR TESTING USE ONLY</strong></p>
 * <p>This mapping allows test scenarios to dynamically route notifications to different
 * Gov.uk Notify API keys based on regex pattern matching against the reference field.</p>
 *
 * <p>By default, all notifications use the standard API key from application.properties.
 * Mappings are only applied when a reference matches a configured regex pattern.</p>
 */
public class ApiKeyMapping {

    private final UUID id;
    private final String regexPattern;
    private final Pattern compiledPattern;
    private final String apiKey;
    private final String description;
    private final LocalDateTime createdAt;

    /**
     * Creates a new API key mapping.
     *
     * @param regexPattern the regex pattern to match against notification references
     * @param apiKey the Gov.uk Notify API key to use when pattern matches
     * @param description explanation of why this mapping exists (e.g., "For E2E test coverage")
     * @throws java.util.regex.PatternSyntaxException if the regex pattern is invalid
     */
    public ApiKeyMapping(String regexPattern, String apiKey, String description) {
        this.id = UUID.randomUUID();
        this.regexPattern = regexPattern;
        this.compiledPattern = Pattern.compile(regexPattern);
        this.apiKey = apiKey;
        this.description = description;
        this.createdAt = LocalDateTime.now();
    }

    /**
     * Creates a new API key mapping with a specific ID (for loading pre-defined mappings).
     *
     * @param id the unique identifier
     * @param regexPattern the regex pattern to match against notification references
     * @param apiKey the Gov.uk Notify API key to use when pattern matches
     * @param description explanation of why this mapping exists
     * @param createdAt the creation timestamp
     */
    public ApiKeyMapping(UUID id, String regexPattern, String apiKey, String description, LocalDateTime createdAt) {
        this.id = id;
        this.regexPattern = regexPattern;
        this.compiledPattern = Pattern.compile(regexPattern);
        this.apiKey = apiKey;
        this.description = description;
        this.createdAt = createdAt;
    }

    /**
     * Checks if the given reference matches this mapping's regex pattern.
     *
     * @param reference the notification reference to test
     * @return true if the reference matches the pattern, false otherwise
     */
    public boolean matches(String reference) {
        if (reference == null) {
            return false;
        }
        return compiledPattern.matcher(reference).matches();
    }

    public UUID getId() {
        return id;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public String getApiKey() {
        return apiKey;
    }

    public String getDescription() {
        return description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    @Override
    public String toString() {
        return "ApiKeyMapping{" +
                "id=" + id +
                ", regexPattern='" + regexPattern + '\'' +
                ", apiKey='" + maskApiKey(apiKey) + '\'' +
                ", description='" + description + '\'' +
                ", createdAt=" + createdAt +
                '}';
    }

    private String maskApiKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }
}
