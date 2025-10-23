package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ApiKeyMappingConstants;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ApiKeyMapping;
import uk.gov.service.notify.NotificationClient;

import jakarta.annotation.PostConstruct;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.regex.PatternSyntaxException;

/**
 * Service for managing API key mappings in memory.
 *
 * <p><strong>FOR TESTING USE ONLY</strong></p>
 *
 * <p>This service maintains an in-memory cache of regex pattern to API key mappings.
 * When a notification is sent, the reference field is checked against all patterns.
 * If a match is found, the corresponding API key is used instead of the default one.</p>
 *
 * <p>Features:</p>
 * <ul>
 *   <li>Thread-safe in-memory storage using ConcurrentHashMap</li>
 *   <li>NotificationClient caching per API key to avoid recreating clients</li>
 *   <li>Pre-defined mappings loaded at startup from ApiKeyMappingConstants</li>
 *   <li>Dynamic mappings can be added/removed via REST API</li>
 * </ul>
 */
@Service
public class ApiKeyMappingService {

    private static final Logger LOGGER = LoggerFactory.getLogger(ApiKeyMappingService.class);

    private final Map<UUID, ApiKeyMapping> mappings = new ConcurrentHashMap<>();
    private final Set<UUID> predefinedMappingIds = ConcurrentHashMap.newKeySet();
    private final Map<String, NotificationClient> clientCache = new ConcurrentHashMap<>();

    /**
     * Initializes the service by loading pre-defined mappings from constants.
     */
    @PostConstruct
    public void init() {
        List<ApiKeyMapping> defaultMappings = ApiKeyMappingConstants.DEFAULT_MAPPINGS;

        if (!defaultMappings.isEmpty()) {
            LOGGER.warn("  Loading {} pre-defined API key mapping(s) - FOR TESTING ONLY", defaultMappings.size());

            for (ApiKeyMapping mapping : defaultMappings) {
                mappings.put(mapping.getId(), mapping);
                predefinedMappingIds.add(mapping.getId());
                LOGGER.warn("  Pre-defined mapping loaded: pattern='{}', description='{}'",
                    mapping.getRegexPattern(), mapping.getDescription());
            }
        } else {
            LOGGER.info("No pre-defined API key mappings configured (default behavior - uses standard API key)");
        }
    }

    /**
     * Adds a new API key mapping.
     *
     * @param regexPattern the regex pattern to match against references
     * @param apiKey the Gov.uk Notify API key to use when pattern matches
     * @param description explanation of why this mapping exists
     * @return the created mapping
     * @throws PatternSyntaxException if the regex pattern is invalid
     * @throws IllegalArgumentException if any parameter is null or blank
     */
    public ApiKeyMapping addMapping(String regexPattern, String apiKey, String description) {
        validateParameters(regexPattern, apiKey, description);

        ApiKeyMapping mapping = new ApiKeyMapping(regexPattern, apiKey, description);
        mappings.put(mapping.getId(), mapping);

        LOGGER.info("Added API key mapping: id={}, pattern='{}', description='{}'", // NOSONAR
            mapping.getId(), regexPattern, description);                            // NOSONAR

        return mapping;
    }

    /**
     * Gets all current API key mappings (both pre-defined and dynamic).
     *
     * @return list of all mappings
     */
    public List<ApiKeyMapping> getAllMappings() {
        return new ArrayList<>(mappings.values());
    }

    /**
     * Removes an API key mapping by ID.
     *
     * @param id the mapping ID to remove
     * @return true if removed, false if not found
     * @throws IllegalArgumentException if attempting to remove a pre-defined mapping
     */
    public boolean removeMapping(UUID id) {
        if (predefinedMappingIds.contains(id)) {
            throw new IllegalArgumentException(
                "Cannot remove pre-defined mapping " + id + ". Pre-defined mappings are loaded from constants."
            );
        }

        ApiKeyMapping removed = mappings.remove(id);
        if (removed != null) {
            LOGGER.info("Removed API key mapping: id={}, pattern='{}'", id, removed.getRegexPattern());
            return true;
        }

        return false;
    }

    /**
     * Clears all user-added mappings (keeps pre-defined mappings).
     */
    public void clearUserMappings() {
        int removedCount = 0;
        Iterator<Map.Entry<UUID, ApiKeyMapping>> iterator = mappings.entrySet().iterator();

        while (iterator.hasNext()) {
            Map.Entry<UUID, ApiKeyMapping> entry = iterator.next();
            if (!predefinedMappingIds.contains(entry.getKey())) {
                iterator.remove();
                removedCount++;
            }
        }

        LOGGER.info("Cleared {} user-added mapping(s)", removedCount);
    }

    /**
     * Checks if a mapping ID is pre-defined (loaded from constants).
     *
     * @param id the mapping ID
     * @return true if pre-defined, false otherwise
     */
    public boolean isPredefined(UUID id) {
        return predefinedMappingIds.contains(id);
    }

    /**
     * Finds the API key to use for a given reference.
     *
     * <p>Checks all mappings in order and returns the API key from the first matching pattern.
     * If no patterns match, returns null (indicating the default API key should be used).</p>
     *
     * @param reference the notification reference to check
     * @return the matched API key, or null if no patterns match
     */
    public String findMatchingApiKey(String reference) {
        if (reference == null || reference.isBlank()) {
            return null;
        }

        for (ApiKeyMapping mapping : mappings.values()) {
            if (mapping.matches(reference)) {
                LOGGER.debug("Reference '{}' matched pattern '{}' - using mapped API key",
                    reference, mapping.getRegexPattern());
                return mapping.getApiKey();
            }
        }

        LOGGER.debug("Reference '{}' did not match any patterns - using default API key", reference);
        return null;
    }

    /**
     * Gets or creates a NotificationClient for the given API key.
     *
     * <p>Clients are cached to avoid recreating them for every notification.</p>
     *
     * @param apiKey the Gov.uk Notify API key
     * @return the NotificationClient instance
     */
    public NotificationClient getNotificationClient(String apiKey) {
        return clientCache.computeIfAbsent(apiKey, key -> {
            LOGGER.info("Creating new NotificationClient for mapped API key");
            return new NotificationClient(key);
        });
    }

    /**
     * Gets the total number of active mappings.
     *
     * @return the count of mappings
     */
    public int getMappingCount() {
        return mappings.size();
    }

    /**
     * Validates mapping parameters.
     */
    private void validateParameters(String regexPattern, String apiKey, String description) {
        if (regexPattern == null || regexPattern.isBlank()) {
            throw new IllegalArgumentException("Regex pattern cannot be null or blank");
        }
        if (apiKey == null || apiKey.isBlank()) {
            throw new IllegalArgumentException("API key cannot be null or blank");
        }
        if (description == null || description.isBlank()) {
            throw new IllegalArgumentException("Description cannot be null or blank");
        }
    }
}
