package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.sentletterfetcher;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ORIGINAL_SENDING_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import java.io.IOException;
import java.io.InputStream;
import java.time.format.DateTimeFormatter;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterNotFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.TooManyLettersFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonaliser;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.PersonalisationDetailsParser;
import uk.gov.companieshouse.logging.Logger;


/**
 * "Fetches" letter PDF for sent letter assumed to be uniquely identified by the reference.
 * It does so by retrieving the data stored when the letter was sent and using it to regenerate
 * the PDF.
 */
@Component
public class SentLetterFetcher {

    private final NotificationDatabaseService notificationDatabaseService;
    private final TemplatePersonaliser templatePersonaliser;
    private final HtmlPdfGenerator pdfGenerator;
    private final PersonalisationDetailsParser parser;
    private final Logger logger;

    public SentLetterFetcher(final NotificationDatabaseService notificationDatabaseService,
                             final TemplatePersonaliser templatePersonaliser,
                             final HtmlPdfGenerator pdfGenerator,
                             final PersonalisationDetailsParser parser,
                             final Logger logger) {
        this.notificationDatabaseService = notificationDatabaseService;
        this.templatePersonaliser = templatePersonaliser;
        this.pdfGenerator = pdfGenerator;
        this.parser = parser;
        this.logger = logger;
    }

    public InputStream fetchLetter(final String reference, final String contextId)
            throws IOException {

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
                parser.parsePersonalisationDetails(personalisationDetailsString, contextId);
        var address = letter.getRecipientDetails().getPhysicalAddress();
        var originalSendingDate = letter.getCreatedAt();

        personalisationDetails.put(ORIGINAL_SENDING_DATE,
                originalSendingDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

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

    public InputStream fetchLetter(
            final String pscName,
            final String companyNumber,
            final String templateId,
            final String letterSendingDate,
            final String contextId)
            throws IOException {

        // TODO DEEP-428 Remove reference?
        var reference = "dummy reference";
        var letter = fetchLetter(pscName, companyNumber, templateId, letterSendingDate);
        var appId = letter.getSenderDetails().getAppId();
        var templateVersion = letter.getLetterDetails().getTemplateVersion();
        var personalisationDetailsString = letter.getLetterDetails().getPersonalisationDetails();
        var personalisationDetails =
                parser.parsePersonalisationDetails(personalisationDetailsString, contextId);
        var address = letter.getRecipientDetails().getPhysicalAddress();
        var originalSendingDate = letter.getCreatedAt();

        personalisationDetails.put(ORIGINAL_SENDING_DATE,
                originalSendingDate.format(DateTimeFormatter.ofPattern("dd MMMM yyyy")));

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
                    "Responding with regenerated letter PDF to view for letter with "
                    + queryParameters(pscName, companyNumber, templateId, letterSendingDate),
                    createLogMap(contextId, "view_letter"));
            return precompiledPdf;
        }
    }

    private GovUkLetterDetailsRequest fetchLetter(final String pscName,
                                                  final String companyNumber,
                                                  final String templateId,
                                                  final String letterSendingDate) {
        var letters = notificationDatabaseService.getLettersByNameCompanyTemplateDate(
                pscName,
                companyNumber,
                templateId,
                letterSendingDate);
        if (letters.isEmpty()) {
            throw new LetterNotFoundException("Letter not found for "
                    + queryParameters(pscName, companyNumber, templateId, letterSendingDate));
        } else if (letters.size() > 1) {
            throw new TooManyLettersFoundException("Multiple letters found for "
                    + queryParameters(pscName, companyNumber, templateId, letterSendingDate));
        }

        return letters.getFirst().getRequest();
    }

    private String queryParameters(final String pscName,
                                   final String companyNumber,
                                   final String templateId,
                                   final String letterSendingDate) {
        return "psc name " + pscName
                + ", companyNumber " + companyNumber
                + ", templateId " + templateId
                + ", letter sending date " + letterSendingDate + ".";
    }
}
