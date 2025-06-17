package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher;

import static java.lang.Boolean.parseBoolean;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.IS_WELSH;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.math.BigDecimal;
import java.util.Arrays;
import java.util.Collections;
import java.util.Map;
import org.apache.commons.lang.StringUtils;
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
 * It also decides whether the letter content should be "doubled up" with a Welsh version
 * too.
 */
@Component
public class LetterDispatcher {

    private static final String WELSH_SUFFIX = "_welsh";

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

        var personalisationDetails =
                getPersonalisationDetails(personalisationDetailsString, contextId);
        var letter = personaliseLetter(
                reference,
                appId,
                templateId,
                templateVersion,
                address,
                personalisationDetails);
        return sendLetterPdf(reference, contextId, letter);
    }

    private String personaliseLetter(
            final String reference,
            final String appId,
            final String templateId,
            final BigDecimal templateVersion,
            final Address address,
            final Map<String, String> personalisationDetails) {

        var letter = templatePersonaliser.personaliseLetterTemplate(
                new LetterTemplateKey(
                        appId,
                        templateId,
                        // Ensure versions "1" and "1.0" are treated as being the same.
                        templateVersion.stripTrailingZeros()),
                        reference,
                        personalisationDetails,
                        address);

        if (getIsWelsh(personalisationDetails)) {
            var welsh = templatePersonaliser.personaliseLetterTemplate(
                    new LetterTemplateKey(
                            appId,
                            templateId + WELSH_SUFFIX,
                            // Ensure versions "1" and "1.0" are treated as being the same.
                            templateVersion.stripTrailingZeros()),
                    reference,
                    personalisationDetails,
                    address);

            // We remove last line of first HTML doc and first 2 lines of second HTML doc
            // so that there is a single HTML document formed by their union.
            letter = removeLastLine(letter) + removeFirstTwoLines(welsh);
        }

        return letter;
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

    private Map<String, String> getPersonalisationDetails(final String personalisationDetailsString,
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
        return personalisationDetails;
    }

    private boolean getIsWelsh(final Map<String, String> personalisationDetails) {
        return personalisationDetails.containsKey(IS_WELSH)
                && parseBoolean(personalisationDetails.get(IS_WELSH));
    }

    private static String removeLastLine(final String html) {
        var lines = StringUtils.split(html, "\n");
        var tailedLines = Arrays.copyOf(lines, lines.length - 1);
        return StringUtils.join(tailedLines, "\n");
    }

    private static String removeFirstTwoLines(final String html) {
        var lines = StringUtils.split(html, "\n");
        var list = Arrays.asList(lines);
        Collections.reverse(list);
        var tailedReversedLines = Arrays.copyOf(lines, lines.length - 2);
        var tailedReversedLinesList = Arrays.asList(tailedReversedLines);
        Collections.reverse(tailedReversedLinesList);
        var toppedLines = tailedReversedLinesList.toArray();
        return StringUtils.join(toppedLines, "\n");
    }


}
