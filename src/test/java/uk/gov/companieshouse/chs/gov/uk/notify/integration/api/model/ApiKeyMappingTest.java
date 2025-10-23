package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ApiKeyMappingTest {

    @ParameterizedTest
    @CsvSource({
            "TEST-123,true",
            "INVALID,false"
    })
    void testMatchesAndToString(String reference, boolean expected) {
        ApiKeyMapping mapping = new ApiKeyMapping("^TEST-\\d+$", "abcd1234EFGH5678", "Simple test");

        assertEquals(expected, mapping.matches(reference));

        String output = mapping.toString();
        assertTrue(output.contains("regexPattern='^TEST-\\d+$'"));
        assertTrue(output.contains("description='Simple test'"));
        assertTrue(output.contains("abcd...5678")); // masked key
        assertFalse(output.contains("abcd1234EFGH5678")); // raw key never appears
    }

    @Test
    void testMatchesWithNullReference() {
        ApiKeyMapping mapping = new ApiKeyMapping("^TEST-\\d+$", "abcd1234EFGH5678", "Simple test");

        assertFalse(mapping.matches(null));
    }
}