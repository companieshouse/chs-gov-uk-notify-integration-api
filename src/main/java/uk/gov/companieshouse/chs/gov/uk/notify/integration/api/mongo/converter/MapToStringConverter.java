package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;

import java.util.Map;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.core.convert.ConversionFailedException;
import org.springframework.core.convert.TypeDescriptor;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.WritingConverter;
import org.springframework.lang.Nullable;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@WritingConverter
public class MapToStringConverter implements Converter<Map<String, Object>, String> {

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Nullable
    @Override
    public String convert(Map<String, Object> source) {
        try {
            return OBJECT_MAPPER.writeValueAsString(source);
        } catch (JsonProcessingException e) {
            LOGGER.error("Error converting Map to String", e);
            throw new ConversionFailedException(
                    TypeDescriptor.valueOf(Map.class),
                    TypeDescriptor.valueOf(String.class), source, e);
        }
    }

}
