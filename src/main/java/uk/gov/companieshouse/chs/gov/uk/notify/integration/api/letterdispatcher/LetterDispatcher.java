package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonaliser;
import uk.gov.companieshouse.logging.Logger;

/**
 * Responsible for the creation and sending of letter PDFs through the Gov Notify service.
 */
@Component
public class LetterDispatcher {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final GovUkNotifyService govUkNotifyService;
    private final NotificationDatabaseService notificationDatabaseService;
    private final TemplatePersonaliser templatePersonaliser;
    private final HtmlPdfGenerator pdfGenerator;
    private final Logger logger;

    public LetterDispatcher(GovUkNotifyService govUkNotifyService,
                            NotificationDatabaseService notificationDatabaseService,
                            TemplatePersonaliser templatePersonaliser,
                            HtmlPdfGenerator pdfGenerator,
                            Logger logger) {
        this.govUkNotifyService = govUkNotifyService;
        this.notificationDatabaseService = notificationDatabaseService;
        this.templatePersonaliser = templatePersonaliser;
        this.pdfGenerator = pdfGenerator;
        this.logger = logger;
    }

    public GovUkNotifyService.LetterResp sendLetter(
            final String reference,
            final String appId,
            final String templateId,
            final BigDecimal templateVersion,
            final Address address,
            final String personalisationDetailsString,
            final String contextId) throws IOException {
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
                new LetterTemplateKey(
                        appId,
                        templateId,
                        // Ensure versions "1" and "1.0" are treated as being the same.
                        templateVersion.stripTrailingZeros()),
                        reference,
                        personalisationDetails,
                        address);
    }

    private GovUkNotifyService.LetterResp
            sendLetterPdf(
                        final String reference,
                        final String contextId,
                        final String letter) throws IOException {

        try (var precompiledPdf = pdfGenerator.generatePdfFromHtml(letter, reference)) {

            var response =
                    govUkNotifyService.sendLetter(
                            reference,
                            precompiledPdf);

            logger.debug("Storing letter response in database",
                    createLogMap(contextId, "store_letter_response"));
            notificationDatabaseService.storeResponse(response);

            return response;
        }

    }

}
