package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Response DTO for API key mapping operations.
 *
 * <p><strong>FOR TESTING USE ONLY</strong></p>
 */
@Schema(description = "API key mapping response")
public class ApiKeyMappingResponse {

    @JsonProperty("id")
    @Schema(description = "Unique identifier for this mapping", example = "550e8400-e29b-41d4-a716-446655440000")
    private UUID id;

    @JsonProperty("regex_pattern")
    @Schema(description = "Regular expression pattern that matches notification references", example = "^e2e-test-.*")
    private String regexPattern;

    @JsonProperty("api_key_preview")
    @Schema(description = "Masked preview of the API key (for security)", example = "test...9012")
    private String apiKeyPreview;

    @JsonProperty("description")
    @Schema(description = "Explanation of why this mapping exists", example = "For Team Poseidon E2E test coverage")
    private String description;

    @JsonProperty("created_at")
    @Schema(description = "When this mapping was created", example = "2025-10-23T14:30:00")
    private LocalDateTime createdAt;

    @JsonProperty("predefined")
    @Schema(description = "Whether this mapping was loaded from constants (cannot be deleted)", example = "false")
    private boolean isPredefined;

    public ApiKeyMappingResponse(UUID id, String regexPattern, String apiKeyPreview, String description,
                                 LocalDateTime createdAt, boolean isPredefined) {
        this.id = id;
        this.regexPattern = regexPattern;
        this.apiKeyPreview = apiKeyPreview;
        this.description = description;
        this.createdAt = createdAt;
        this.isPredefined = isPredefined;
    }

    /**
     * Creates a response DTO from an ApiKeyMapping, masking the API key for security.
     *
     * @param mapping the mapping to convert
     * @param isPredefined whether this is a pre-defined mapping
     * @return the response DTO
     */
    public static ApiKeyMappingResponse from(ApiKeyMapping mapping, boolean isPredefined) {
        String maskedKey = maskApiKey(mapping.getApiKey());
        return new ApiKeyMappingResponse(
            mapping.getId(),
            mapping.getRegexPattern(),
            maskedKey,
            mapping.getDescription(),
            mapping.getCreatedAt(),
            isPredefined
        );
    }

    private static String maskApiKey(String key) {
        if (key == null || key.length() < 8) {
            return "***";
        }
        return key.substring(0, 4) + "..." + key.substring(key.length() - 4);
    }

    public UUID getId() {
        return id;
    }

    public void setId(UUID id) {
        this.id = id;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public String getApiKeyPreview() {
        return apiKeyPreview;
    }

    public void setApiKeyPreview(String apiKeyPreview) {
        this.apiKeyPreview = apiKeyPreview;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public boolean isPredefined() {
        return isPredefined;
    }

    public void setPredefined(boolean predefined) {
        isPredefined = predefined;
    }
}
