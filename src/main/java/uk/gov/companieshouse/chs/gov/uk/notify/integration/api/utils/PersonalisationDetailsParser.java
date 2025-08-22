package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.logging.Logger;


@Component
public class PersonalisationDetailsParser {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final Logger logger;

    public PersonalisationDetailsParser(Logger logger) {
        this.logger = logger;
    }

    public Map<String, String> parsePersonalisationDetails(
            final String personalisationDetailsString,
            final String contextId) {

        Map<java.lang.String, java.lang.String> personalisationDetails;
        try {
            logger.debug("Parsing personalisation details",
                    createLogMap(contextId, "parse_details"));
            personalisationDetails = OBJECT_MAPPER.readValue(
                    personalisationDetailsString,
                    new TypeReference<>() {}
            );
        } catch (
                JsonProcessingException jpe) {
            var message = "Failed to parse personalisation details: " + jpe.getMessage();
            logger.error(message, createLogMap(contextId, "parse_error"));
            throw new LetterValidationException(message);
        }
        return personalisationDetails;
    }

}
