package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;

import jakarta.validation.constraints.NotBlank;

/**
 * Request DTO for creating a new API key mapping.
 *
 * <p><strong>FOR TESTING USE ONLY</strong></p>
 */
@Schema(description = "Request to create a new API key mapping for testing purposes")
public class ApiKeyMappingRequest {

    @NotBlank(message = "Regex pattern is required")
    @JsonProperty("regex_pattern")
    @Schema(
        description = "Regular expression pattern to match against notification reference field",
        example = "^e2e-test-.*"
    )
    private String regexPattern;

    @NotBlank(message = "API key is required")
    @JsonProperty("api_key")
    @Schema(
        description = "The Gov.uk Notify API key to use when the pattern matches",
        example = "test-12345678-1234-1234-1234-123456789012-12345678-1234-1234-1234-123456789012"
    )
    private String apiKey;

    @NotBlank(message = "Description is required")
    @JsonProperty("description")
    @Schema(
        description = "Explanation of why this mapping exists (e.g., 'For Team Poseidon E2E tests')",
        example = "For Team Poseidon E2E test coverage using dedicated API key"
    )
    private String description;

    public ApiKeyMappingRequest(String regexPattern, String apiKey, String description) {
        this.regexPattern = regexPattern;
        this.apiKey = apiKey;
        this.description = description;
    }

    public String getRegexPattern() {
        return regexPattern;
    }

    public void setRegexPattern(String regexPattern) {
        this.regexPattern = regexPattern;
    }

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }
}
