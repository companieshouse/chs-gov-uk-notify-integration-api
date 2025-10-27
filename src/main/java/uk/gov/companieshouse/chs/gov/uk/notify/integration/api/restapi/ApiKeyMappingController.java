package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ApiKeyMapping;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ApiKeyMappingRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ApiKeyMappingResponse;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.ApiKeyMappingService;

import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

/**
 * REST Controller for managing API key mappings.
 *
 * <p><strong>FOR INTERNAL TESTING USE ONLY</strong></p>
 *
 * <p>This controller provides endpoints to dynamically configure which Gov.uk Notify API key
 * should be used based on regex pattern matching against the notification reference field.</p>
 *
 * <p><strong>IMPORTANT:</strong> By default, all notifications use the standard API key from
 * application.properties. These mappings are ONLY applied when a reference matches a pattern.</p>
 *
 * <p><strong>Security:</strong> All endpoints require the X-Internal-Api-Key header for basic
 * authentication to prevent accidental usage.</p>
 *
 * <p><strong>Typical Use Cases:</strong></p>
 * <ul>
 *   <li>E2E test scenarios that need to use a specific team's Gov.uk Notify account</li>
 *   <li>Testing with multiple API keys without redeploying the application</li>
 *   <li>Isolating test traffic to dedicated test accounts</li>
 * </ul>
 *
 * @see ApiKeyMappingService
 */
@RestController
@RequestMapping("/gov-uk-notify-integration/api-key-mappings")
@Tag(name = "API Key Mappings (Internal Testing)", description = "TESTING ONLY - Manage dynamic API key routing")
public class ApiKeyMappingController {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyMappingController.class);
    
    private static final String INVALID_OR_MISSING_X_INTERNAL_API_KEY_HEADER = "Invalid or missing X-Internal-Api-Key header";
    private static final String ERROR = "error";

    private final ApiKeyMappingService apiKeyMappingService;
    private final String internalApiKey;

    public ApiKeyMappingController(
        ApiKeyMappingService apiKeyMappingService,
        @Value("${internal.api.key:default-test-key}") String internalApiKey
    ) {
        this.apiKeyMappingService = apiKeyMappingService;
        this.internalApiKey = internalApiKey;
    }
    

    /**
     * Lists all active API key mappings.
     *
     * @param apiKey the internal API key for authentication
     * @return list of all mappings (both pre-defined and user-added)
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "List all API key mappings",
        description = "Returns all active mappings including pre-defined and user-added ones. API keys are masked for security."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "200", description = "Successfully retrieved mappings"),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing X-Internal-Api-Key header", content = @Content)
    })
    public ResponseEntity<?> getAllMappings( // NOSONAR
        @Parameter(description = "Internal API key for authentication", required = true)
        @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        if (unauthenticated(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(ERROR, INVALID_OR_MISSING_X_INTERNAL_API_KEY_HEADER));
        }

        List<ApiKeyMapping> mappings = apiKeyMappingService.getAllMappings();
        List<ApiKeyMappingResponse> responses = mappings.stream()
            .map(m -> ApiKeyMappingResponse.from(m, apiKeyMappingService.isPredefined(m.getId())))
            .toList();

        LOGGER.info("Retrieved {} API key mapping(s)", responses.size());

        return ResponseEntity.ok(Map.of(
            "total_count", responses.size(),
            "mappings", responses
        ));
    }

    /**
     * Creates a new API key mapping.
     *
     * @param apiKey the internal API key for authentication
     * @param request the mapping details
     * @return the created mapping
     */
    @PostMapping(consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(
        summary = "Create a new API key mapping",
        description = "Adds a new regex pattern to API key mapping. When a notification reference matches the pattern, the specified API key will be used."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "201", description = "Mapping created successfully"),
        @ApiResponse(responseCode = "400", description = "Invalid request - bad regex pattern or missing fields", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing X-Internal-Api-Key header", content = @Content)
    })
    public ResponseEntity<?> createMapping( // NOSONAR
        @Parameter(description = "Internal API key for authentication", required = true)
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @Valid @RequestBody ApiKeyMappingRequest request
    ) {
        if (unauthenticated(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(ERROR, INVALID_OR_MISSING_X_INTERNAL_API_KEY_HEADER));
        }

        try {
            ApiKeyMapping mapping = apiKeyMappingService.addMapping(
                request.getRegexPattern(),
                request.getApiKey(),
                request.getDescription()
            );

            ApiKeyMappingResponse response = ApiKeyMappingResponse.from(mapping, false);

            LOGGER.info("Created new API key mapping: id={}, pattern='{}'", // NOSONAR 
                mapping.getId(), request.getRegexPattern());                // NOSONAR 

            return ResponseEntity.status(HttpStatus.CREATED).body(response);

        } catch (PatternSyntaxException e) {
            LOGGER.warn("Invalid regex pattern: {}", request.getRegexPattern(), e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(
                        ERROR, "Invalid regex pattern",
                    "message", e.getMessage()
                ));
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Invalid mapping parameters", e);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR, e.getMessage()));
        }
    }

    /**
     * Deletes a specific API key mapping.
     *
     * @param apiKey the internal API key for authentication
     * @param id the mapping ID to delete
     * @return success or error response
     */
    @DeleteMapping("/{id}")
    @Operation(
        summary = "Delete an API key mapping",
        description = "Removes a user-added mapping. Pre-defined mappings cannot be deleted."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "Mapping deleted successfully", content = @Content),
        @ApiResponse(responseCode = "400", description = "Cannot delete pre-defined mapping", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing X-Internal-Api-Key header", content = @Content),
        @ApiResponse(responseCode = "404", description = "Mapping not found", content = @Content)
    })
    public ResponseEntity<?> deleteMapping( // NOSONAR
        @Parameter(description = "Internal API key for authentication", required = true)
        @RequestHeader("X-Internal-Api-Key") String apiKey,
        @Parameter(description = "Mapping ID to delete", required = true)
        @PathVariable UUID id
    ) {
        if (unauthenticated(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(ERROR, INVALID_OR_MISSING_X_INTERNAL_API_KEY_HEADER));
        }

        try {
            boolean removed = apiKeyMappingService.removeMapping(id);
            if (removed) {
                LOGGER.info("Deleted API key mapping: id={}", id);
                return ResponseEntity.noContent().build();
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body(Map.of(ERROR, "Mapping not found with id: " + id));
            }
        } catch (IllegalArgumentException e) {
            LOGGER.warn("Attempted to delete pre-defined mapping: id={}", id);
            return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of(ERROR, e.getMessage()));
        }
    }

    /**
     * Clears all user-added mappings (keeps pre-defined mappings).
     *
     * @param apiKey the internal API key for authentication
     * @return success response
     */
    @DeleteMapping
    @Operation(
        summary = "Clear all user-added mappings",
        description = "Removes all user-added mappings while keeping pre-defined ones. Useful for cleaning up after tests."
    )
    @ApiResponses(value = {
        @ApiResponse(responseCode = "204", description = "User mappings cleared successfully", content = @Content),
        @ApiResponse(responseCode = "401", description = "Unauthorized - invalid or missing X-Internal-Api-Key header", content = @Content)
    })
    public ResponseEntity<?> clearUserMappings( // NOSONAR
        @Parameter(description = "Internal API key for authentication", required = true)
        @RequestHeader("X-Internal-Api-Key") String apiKey
    ) {
        if (unauthenticated(apiKey)) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                .body(Map.of(ERROR, INVALID_OR_MISSING_X_INTERNAL_API_KEY_HEADER));
        }

        apiKeyMappingService.clearUserMappings();
        LOGGER.info("Cleared all user-added API key mappings");

        return ResponseEntity.noContent().build();
    }

    /**
     * Authenticates the request using the X-Internal-Api-Key header.
     */
    private boolean unauthenticated(String providedKey) {
        if (providedKey == null || providedKey.isBlank()) {
            LOGGER.warn("Missing X-Internal-Api-Key header");
            return true;
        }

        boolean valid = internalApiKey.equals(providedKey);
        if (!valid) {
            LOGGER.warn("Invalid X-Internal-Api-Key provided");
        }

        return !valid;
    }
}
