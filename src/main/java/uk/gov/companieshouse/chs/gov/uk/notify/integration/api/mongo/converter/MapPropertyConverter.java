package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter;


import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.data.convert.PropertyValueConverter;
import org.springframework.data.mongodb.core.convert.MongoConversionContext;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@Component
public class MapPropertyConverter implements PropertyValueConverter<Map<String, Object>, String, MongoConversionContext> {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private final ObjectMapper objectMapper;

    public MapPropertyConverter() {
        this.objectMapper = new ObjectMapper();
    }

    public MapPropertyConverter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Nullable
    @Override
    public Map<String, Object> read(String value, MongoConversionContext context) {
        try {
            return objectMapper.readValue(value, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting String to Map", e);
            throw new ConversionFailedException(
                    TypeDescriptor.valueOf(String.class),
                    TypeDescriptor.valueOf(Map.class), value, e);
        }
    }

    @Nullable
    @Override
    public String write(Map<String, Object> value, MongoConversionContext context) {
        try {
            return objectMapper.writeValueAsString(value);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting Map to String", e);
            throw new ConversionFailedException(
                    TypeDescriptor.valueOf(Map.class),
                    TypeDescriptor.valueOf(String.class), value, e);
        }
    }
}
