package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter;

import static net.javacrumbs.jsonunit.assertj.JsonAssertions.assertThatJson;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

import java.util.Map;

import com.fasterxml.jackson.core.JsonParseException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.data.mongodb.core.convert.MongoConversionContext;

class MapPropertyConverterTest {

    private MapPropertyConverter mapPropertyConverter;
    private MongoConversionContext mongoConversionContext;

    @BeforeEach
    void setUp() {
        mapPropertyConverter = new MapPropertyConverter(new ObjectMapper());
        mongoConversionContext = mock(MongoConversionContext.class);
    }

    @Test
    void shouldConvertStringIntoMap() {
        // When
        assertThat(mapPropertyConverter.read("{\"key1\":\"value1\",\"key2\":2}", mongoConversionContext))
                .containsEntry("key1", "value1")
                .containsEntry("key2", 2);
    }

    @Test
    void shouldConvertMapToJsonString() {
        // When
        assertThatJson(mapPropertyConverter.write(Map.of("key1", "value1", "key2", 2), mongoConversionContext))
                .isObject()
                .containsEntry("key1", "value1")
                .containsEntry("key2", 2);
    }

    @Test
    void shouldThrowConversionExceptionGivenInvalidString() {
        assertThatThrownBy(() -> mapPropertyConverter.read("invalid", mongoConversionContext))
                .isInstanceOf(ConversionFailedException.class)
                .hasMessage("Failed to convert from type [java.lang.String] to type [java.util.Map<?, ?>] for value [invalid]");
    }

    @Test
    void shouldThrowConversionExceptionGivenInvalidMap() throws Exception {
        // Given
        ObjectMapper mock = mock(ObjectMapper.class);
        Map<String, Object> personalisationMap = Map.of("key1", "value1", "key2", 2);

        given(mock.writeValueAsString(personalisationMap)).willThrow(new JsonParseException("error"));
        mapPropertyConverter = new MapPropertyConverter(mock);

        // When & Then
        assertThatThrownBy(() -> mapPropertyConverter.write(personalisationMap, mongoConversionContext))
                .isInstanceOf(ConversionFailedException.class)
                .hasMessage("Failed to convert from type [java.util.Map<?, ?>] to type [java.lang.String] for value [{...}]");
    }
}
