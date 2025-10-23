package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ApiKeyMapping;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ApiKeyMappingRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.ApiKeyMappingService;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.regex.PatternSyntaxException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class ApiKeyMappingControllerTest {

    @Mock
    private ApiKeyMappingService apiKeyMappingService;

    @InjectMocks
    private ApiKeyMappingController controller;

    private static final String VALID_API_KEY = "test-internal-key";
    private static final String INVALID_API_KEY = "wrong-key";

    @BeforeEach
    void setUp() {
        ReflectionTestUtils.setField(controller, "internalApiKey", VALID_API_KEY);
    }

    @Test
    void testGetAllMappings_Success() {
        // Given
        ApiKeyMapping mapping = new ApiKeyMapping("^test-.*", "api-key-123", "Test mapping");
        when(apiKeyMappingService.getAllMappings()).thenReturn(List.of(mapping));
        when(apiKeyMappingService.isPredefined(mapping.getId())).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.getAllMappings(VALID_API_KEY);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(response.getBody()).isInstanceOf(Map.class);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("total_count", 1);
        assertThat(body.get("mappings")).isNotNull();

        verify(apiKeyMappingService).getAllMappings();
    }

    @Test
    void testGetAllMappings_Empty() {
        // Given
        when(apiKeyMappingService.getAllMappings()).thenReturn(Collections.emptyList());

        // When
        ResponseEntity<?> response = controller.getAllMappings(VALID_API_KEY);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.OK);

        @SuppressWarnings("unchecked")
        Map<String, Object> body = (Map<String, Object>) response.getBody();
        assertThat(body).containsEntry("total_count", 0);
        

        verify(apiKeyMappingService).getAllMappings();
    }

    @Test
    void testGetAllMappings_Unauthorized() {
        // When
        ResponseEntity<?> response = controller.getAllMappings(INVALID_API_KEY);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).contains("Invalid or missing X-Internal-Api-Key");

        verify(apiKeyMappingService, never()).getAllMappings();
    }

    @Test
    void testGetAllMappings_MissingApiKey() {
        // When
        ResponseEntity<?> response = controller.getAllMappings(null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(apiKeyMappingService, never()).getAllMappings();
    }

    @Test
    void testGetAllMappings_BlankApiKey() {
        // When
        ResponseEntity<?> response = controller.getAllMappings("  ");

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(apiKeyMappingService, never()).getAllMappings();
    }

    @Test
    void testCreateMapping_Success() {
        // Given
        ApiKeyMappingRequest request = new ApiKeyMappingRequest(
            "^e2e-test-.*",
            "test-api-key-123",
            "For E2E tests"
        );
        ApiKeyMapping mapping = new ApiKeyMapping(
            request.getRegexPattern(),
            request.getApiKey(),
            request.getDescription()
        );
        when(apiKeyMappingService.addMapping(
            request.getRegexPattern(),
            request.getApiKey(),
            request.getDescription()
        )).thenReturn(mapping);

        // When
        ResponseEntity<?> response = controller.createMapping(VALID_API_KEY, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(response.getBody()).isNotNull();

        verify(apiKeyMappingService).addMapping(
            request.getRegexPattern(),
            request.getApiKey(),
            request.getDescription()
        );
    }

    @Test
    void testCreateMapping_InvalidRegex() {
        // Given
        ApiKeyMappingRequest request = new ApiKeyMappingRequest(
            "[invalid(regex",
            "test-api-key-123",
            "Invalid pattern"
        );
        when(apiKeyMappingService.addMapping(anyString(), anyString(), anyString()))
            .thenThrow(new PatternSyntaxException("Invalid regex", "[invalid(regex", 0));

        // When
        ResponseEntity<?> response = controller.createMapping(VALID_API_KEY, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body).containsEntry("error", "Invalid regex pattern");
    }

    @Test
    void testCreateMapping_InvalidParameters() {
        // Given
        ApiKeyMappingRequest request = new ApiKeyMappingRequest(
            "^test-.*",
            "",
            "Missing API key"
        );
        when(apiKeyMappingService.addMapping(anyString(), anyString(), anyString()))
            .thenThrow(new IllegalArgumentException("API key cannot be null or blank"));

        // When
        ResponseEntity<?> response = controller.createMapping(VALID_API_KEY, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).contains("API key cannot be null or blank");
    }

    @Test
    void testCreateMapping_Unauthorized() {
        // Given
        ApiKeyMappingRequest request = new ApiKeyMappingRequest(
            "^test-.*",
            "api-key",
            "Test"
        );

        // When
        ResponseEntity<?> response = controller.createMapping(INVALID_API_KEY, request);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(apiKeyMappingService, never()).addMapping(anyString(), anyString(), anyString());
    }

    @Test
    void testDeleteMapping_Success() {
        // Given
        UUID id = UUID.randomUUID();
        when(apiKeyMappingService.removeMapping(id)).thenReturn(true);

        // When
        ResponseEntity<?> response = controller.deleteMapping(VALID_API_KEY, id);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(apiKeyMappingService).removeMapping(id);
    }

    @Test
    void testDeleteMapping_NotFound() {
        // Given
        UUID id = UUID.randomUUID();
        when(apiKeyMappingService.removeMapping(id)).thenReturn(false);

        // When
        ResponseEntity<?> response = controller.deleteMapping(VALID_API_KEY, id);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).contains("Mapping not found");

        verify(apiKeyMappingService).removeMapping(id);
    }

    @Test
    void testDeleteMapping_PredefinedMapping() {
        // Given
        UUID id = UUID.randomUUID();
        when(apiKeyMappingService.removeMapping(id))
            .thenThrow(new IllegalArgumentException("Cannot remove pre-defined mapping"));

        // When
        ResponseEntity<?> response = controller.deleteMapping(VALID_API_KEY, id);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);

        @SuppressWarnings("unchecked")
        Map<String, String> body = (Map<String, String>) response.getBody();
        assertThat(body.get("error")).contains("Cannot remove pre-defined mapping");

        verify(apiKeyMappingService).removeMapping(id);
    }

    @Test
    void testDeleteMapping_Unauthorized() {
        // Given
        UUID id = UUID.randomUUID();

        // When
        ResponseEntity<?> response = controller.deleteMapping(INVALID_API_KEY, id);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(apiKeyMappingService, never()).removeMapping(any(UUID.class));
    }

    @Test
    void testClearUserMappings_Success() {
        // When
        ResponseEntity<?> response = controller.clearUserMappings(VALID_API_KEY);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
        verify(apiKeyMappingService).clearUserMappings();
    }

    @Test
    void testClearUserMappings_Unauthorized() {
        // When
        ResponseEntity<?> response = controller.clearUserMappings(INVALID_API_KEY);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(apiKeyMappingService, never()).clearUserMappings();
    }

    @Test
    void testClearUserMappings_NullApiKey() {
        // When
        ResponseEntity<?> response = controller.clearUserMappings(null);

        // Then
        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        verify(apiKeyMappingService, never()).clearUserMappings();
    }
}
