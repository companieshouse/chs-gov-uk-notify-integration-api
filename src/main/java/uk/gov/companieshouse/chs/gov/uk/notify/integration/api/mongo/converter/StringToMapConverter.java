package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import org.springframework.lang.Nullable;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@ReadingConverter
public class StringToMapConverter implements Converter<String, Map<String, Object>> {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Nullable
    @Override
    public Map<String, Object> convert(String source) {
        try {
            return OBJECT_MAPPER.readValue(source, new TypeReference<>() {});
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting String to Map", e);
            throw new ConversionFailedException(
                    TypeDescriptor.valueOf(String.class),
                    TypeDescriptor.valueOf(Map.class), source, e);
        }
    }

}
