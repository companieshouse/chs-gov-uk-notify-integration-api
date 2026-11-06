package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter;


import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import java.util.Map;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.convert.PropertyValueConverter;
import org.springframework.data.mongodb.core.convert.MongoConversionContext;
import tools.jackson.core.JacksonException;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

public class MapPropertyConverter implements PropertyValueConverter<Map<String, Object>, String, MongoConversionContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private final JsonMapper objectMapper;

    public MapPropertyConverter() {
        this.objectMapper = JsonMapper.builder().build();
    }

    public MapPropertyConverter(JsonMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Nullable
    @Override
    public Map<String, Object> read(@NonNull String value, @NonNull MongoConversionContext context) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JacksonException e) {
            LOGGER.error("Error converting String to Map", e);
            throw new ConversionFailedException(
                    TypeDescriptor.valueOf(String.class),
                    TypeDescriptor.valueOf(Map.class), value, e);
        }
    }

    @Nullable
    @Override
    public String write(@NonNull Map<String, Object> value, @NonNull MongoConversionContext context) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JacksonException e) {
            LOGGER.error("Error converting Map to String", e);
            throw new ConversionFailedException(
                    TypeDescriptor.valueOf(Map.class),
                    TypeDescriptor.valueOf(String.class), value, e);
        }
    }
}
