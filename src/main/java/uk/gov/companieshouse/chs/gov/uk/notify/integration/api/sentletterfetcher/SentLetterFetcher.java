package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.sentletterfetcher;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.InputStream;
import java.util.Map;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterNotFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.TooManyLettersFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonaliser;
import uk.gov.companieshouse.logging.Logger;


/**
 * "Fetches" letter PDF for sent letter assumed to be uniquely identified by the reference.
 * It does so by retrieving the data stored when the letter was sent and using it to regenerate
 * the PDF.
 */
@Component
public class SentLetterFetcher {

    private static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private final NotificationDatabaseService notificationDatabaseService;
    private final TemplatePersonaliser templatePersonaliser;
    private final HtmlPdfGenerator pdfGenerator;
    private final Logger logger;

    public SentLetterFetcher(final NotificationDatabaseService notificationDatabaseService,
                             final TemplatePersonaliser templatePersonaliser,
                             final HtmlPdfGenerator pdfGenerator,
                             final Logger logger) {
        this.notificationDatabaseService = notificationDatabaseService;
        this.templatePersonaliser = templatePersonaliser;
        this.pdfGenerator = pdfGenerator;
        this.logger = logger;
    }

    public InputStream fetchLetter(final String reference, final String contextId)
            throws IOException {

        // TODO DEEP-428 Tidy this up?
        var letters = notificationDatabaseService.getLetterByReference(reference);
        if (letters.isEmpty()) {
            throw new LetterNotFoundException("Letter not found for reference: " + reference);
        } else if (letters.size() > 1) {
            throw new TooManyLettersFoundException("Multiple letters found for reference: "
                    + reference);
        }

        var letter = letters.getFirst().getRequest();
        var appId = letter.getSenderDetails().getAppId();
        var templateId = letter.getLetterDetails().getTemplateId();
        var templateVersion = letter.getLetterDetails().getTemplateVersion();
        var personalisationDetailsString = letter.getLetterDetails().getPersonalisationDetails();
        var personalisationDetails =
                parsePersonalisationDetails(personalisationDetailsString, contextId);
        var address = letter.getRecipientDetails().getPhysicalAddress();

        var html = templatePersonaliser.personaliseLetterTemplate(
                new LetterTemplateKey(
                        appId,
                        templateId,
                        templateVersion),
                reference,
                personalisationDetails,
                address);

        try (var precompiledPdf = pdfGenerator.generatePdfFromHtml(html, reference)) {
            logger.debug(
                    "Responding with regenerated letter PDF to view for letter with reference "
                            + reference, createLogMap(contextId, "view_letter"));
            return precompiledPdf;
        }
    }

    private Map<String, String> parsePersonalisationDetails(
            final String personalisationDetailsString,
            final String contextId) {
        Map<String, String> personalisationDetails;
        try {
            logger.debug("Parsing retrieved personalisation details",
                    createLogMap(contextId, "parse_details"));
            personalisationDetails = OBJECT_MAPPER.readValue(
                    personalisationDetailsString,
                    new TypeReference<>() {}
            );
        } catch (JsonProcessingException jpe) {
            var message = "Failed to parse retrieved personalisation details: " + jpe.getMessage();
            logger.error(message, createLogMap(contextId, "parse_error"));
            throw new LetterValidationException(message);
        }
        return personalisationDetails;
    }
}
