package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants;

import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ApiKeyMapping;

import java.util.Collections;
import java.util.List;

/**
 * Pre-defined API key mappings that are loaded at application startup.
 *
 * <p><strong>WARNING: FOR TESTING PURPOSES ONLY</strong></p>
 *
 * <p><strong>IMPORTANT:</strong> By default, the application ALWAYS uses the API key from
 * application.properties (gov.uk.notify.api.key). These mappings are ONLY for specific
 * test scenarios and should NOT be used for regular operation.</p>
 *
 * <p><strong>Guidelines for adding mappings:</strong></p>
 * <ul>
 *   <li>Keep this list EMPTY or MINIMAL in production environments</li>
 *   <li>Only add mappings for specific E2E test cases or dedicated test teams</li>
 *   <li>Always provide a clear description explaining the rationale</li>
 *   <li>Use specific regex patterns that won't accidentally match production references</li>
 *   <li>Coordinate with other teams before adding mappings that may affect their tests</li>
 * </ul>
 *
 * <p><strong>Example usage (commented out):</strong></p>
 * <pre>
 * new ApiKeyMapping(
 *     "^e2e-team-poseidon-.*",
 *     "poseidon-api-key-here",
 *     "For Team Poseidon E2E tests - uses their dedicated Gov.uk Notify account"
 * )
 * </pre>
 */
public class ApiKeyMappingConstants {

    /**
     * Pre-defined mappings loaded at application startup.
     *
     * <p><strong>KEEP THIS LIST EMPTY BY DEFAULT</strong></p>
     *
     * <p>Only add mappings here if you need them to be permanently available for
     * specific test scenarios. For ad-hoc testing, use the REST API endpoints instead.</p>
     */
    public static final List<ApiKeyMapping> DEFAULT_MAPPINGS = Collections.emptyList();

    // sonarignore:start 
    /*
     * COMMENTED EXAMPLE - Uncomment and modify only if you need permanent test mappings:
     *
     * public static final List<ApiKeyMapping> DEFAULT_MAPPINGS = List.of(
     *     new ApiKeyMapping(
     *         "^e2e-test-specific-scenario-.*",
     *         "your-test-api-key-here",
     *         "For specific E2E test scenario XYZ - uses dedicated test API key"
     *     )
     * ); //NOSONAR java:S125
     */
     // sonarignore:end

    private ApiKeyMappingConstants() {
        // Utility class - prevent instantiation
    }
}
