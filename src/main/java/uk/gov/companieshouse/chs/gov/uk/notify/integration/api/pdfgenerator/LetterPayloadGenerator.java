package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonaliser;
import uk.gov.companieshouse.logging.Logger;

@Component
public class LetterPayloadGenerator {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    final GovUkNotifyService govUkNotifyService;
    final NotificationDatabaseService notificationDatabaseService;
    final TemplatePersonaliser templatePersonaliser;
    private final Logger logger;

    public LetterPayloadGenerator(GovUkNotifyService govUkNotifyService,
                                  NotificationDatabaseService notificationDatabaseService,
                                  TemplatePersonaliser templatePersonaliser,
                                  Logger logger) {
        this.govUkNotifyService = govUkNotifyService;
        this.notificationDatabaseService = notificationDatabaseService;
        this.templatePersonaliser = templatePersonaliser;
        this.logger = logger;
    }

    public ResponseEntity<Void> sendLetter(
            final String reference,
            final String appId,
            final String templateId,
            final BigDecimal templateVersion,
            final Address address,
            final String personalisationDetailsString,
            final String contextId) {
        var letter = personaliseLetter(
                reference,
                appId,
                templateId,
                templateVersion,
                address,
                personalisationDetailsString,
                contextId);
        return sendLetterPdf(reference, contextId, letter);
    }

    private String personaliseLetter(
            final String reference,
            final String appId,
            final String templateId,
            final BigDecimal templateVersion,
            final Address address,
            final String personalisationDetailsString,
            final String contextId) {

        Map<String, String> personalisationDetails;
        try {
            logger.debug("Parsing personalisation details",
                    createLogMap(contextId, "parse_details"));
            personalisationDetails = OBJECT_MAPPER.readValue(
                    personalisationDetailsString,
                    new TypeReference<>() {}
            );
        } catch (JsonProcessingException jpe) {
            var message = "Failed to parse personalisation details: " + jpe.getMessage();
            logger.error(message, createLogMap(contextId, "parse_error"));
            throw new LetterValidationException(message);
        }

        return templatePersonaliser.personaliseLetterTemplate(
                new ChLetterTemplate(
                        appId,
                        templateId,
                        // Ensure versions "1" and "1.0" are treated as being the same.
                        templateVersion.stripTrailingZeros()),
                /*senderDetails.getReference()*/reference,
                personalisationDetails,
                address);
    }

    @SuppressWarnings("java:S1135") // TODO left in place intentionally for now.
    private ResponseEntity<Void> sendLetterPdf(// TODO DEEP-287 Method name?
            final String reference,
            final String contextId,
            final String letter) {

        // TODO DEEP-288 Stop logging the entire letter HMTL content.
        logger.info("letter = " + letter);

        try (var precompiledPdf = getPrecompiledPdf()) {

            var letterResp =
                    govUkNotifyService.sendLetter(
                            reference,
                            precompiledPdf);

            logger.debug("Storing letter response in database",
                    createLogMap(contextId, "store_letter_response"));
            notificationDatabaseService.storeResponse(letterResp);

            if (letterResp.success()) {
                logger.info("Letter processed successfully",
                        createLogMap(contextId, "letter_success"));
                return new ResponseEntity<>(HttpStatus.CREATED);
            } else {
                logger.error("Failed to process letter", createLogMap(contextId, "letter_failure"));
                return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
            }

        } catch (IOException ioe) {
            logger.error("Failed to load precompiled letter PDF. Caught IOException: "
                    + ioe.getMessage(), createLogMap(contextId, "load_pdf_error"));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }

    }

    @SuppressWarnings("java:S1135") // TODO left in place intentionally for now.
    public InputStream getPrecompiledPdf() {
        // TODO DEEP-288 Replace temporary test code and remove Demonstrate connectivity.pdf.
        return getClass().getClassLoader().getResourceAsStream("Demonstrate connectivity.pdf");
    }

    private Map<String, Object> createLogMap(final String contextId, final String action) {
        Map<String, Object> logMap = new HashMap<>();
        logMap.put("contextId", contextId);
        logMap.put("action", action);
        return logMap;
    }
}
