package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.sentletterfetcher;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.Constants.DATE_FORMATTER;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.constants.ContextVariables.ORIGINAL_SENDING_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.VIEW_LETTER_PDF;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.VIEW_LETTER_PDFS;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterNotFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterValidationException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.TooManyLettersFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonaliser;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.PersonalisationDetailsParser;
import uk.gov.companieshouse.logging.Logger;

/**
 * "Fetches" letter PDFs for sent letters identified/selected by the criteria provided, one
 * at a time. It does so by retrieving the data stored when the letter was sent and using
 * it to regenerate the PDF.
 */
@Component
public class SentLetterFetcher {

    private final NotificationDatabaseService notificationDatabaseService;
    private final TemplatePersonaliser templatePersonaliser;
    private final HtmlPdfGenerator pdfGenerator;
    private final PersonalisationDetailsParser parser;
    private final Logger logger;

    public record FetchedLetter(InputStream letter, int numberOfLetters) {}

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

    /**
     * "Fetches" letter PDF for sent letter assumed to be uniquely identified by the reference.
     *  It does so by retrieving the data stored when the letter was sent and using it to regenerate
     *  the PDF.
     *
     * @param reference the reference assumed to uniquely identify the letter to be viewed
     * @param contextId unique identifier for tracking the request
     * @return the letter PDF
     * @throws IOException should something unexpected happen
     */
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
        var html = getHtml(letter, reference, contextId);

        try (var precompiledPdf = pdfGenerator.generatePdfFromHtml(html, reference)) {
            logger.debug(
                    "Responding with regenerated letter PDF to view for letter with reference "
                            + reference, createLogMap(contextId, VIEW_LETTER_PDF));
            return precompiledPdf;
        }
    }

    /**
     * "Fetches" letter PDFs for sent letters matched by the reference, one at a time.
     *  It does so by retrieving the data stored when the letter was sent and using it to regenerate
     *  the PDF.
     *
     * @param reference the reference matched against the references on the letters to be viewed
     * @param letterNumber the number of the specific letter to be fetched, from the collection of
     *                     matching letters, ordered by the createdAt date. The first such letter
     *                     is letter number 1.
     * @param contextId unique identifier for tracking the request
     * @return the letter PDF
     * @throws IOException should something unexpected happen
     */
    public FetchedLetter fetchLetter(final String reference,
                                     final int letterNumber,
                                     final String contextId)
            throws IOException {
        validateLetterNumber(letterNumber);
        var page = notificationDatabaseService.getLetterByReference(reference, letterNumber);
        var letter = getLetter(page, letterNumber);
        var letterReference = letter.getSenderDetails().getReference();
        var html = getHtml(letter, letterReference, contextId);

        try (var precompiledPdf = pdfGenerator.generatePdfFromHtml(html, reference)) {
            logger.debug(
                    "Responding with regenerated letter PDF to view for letter number "
                            + letterNumber + " with reference "
                            + reference, createLogMap(contextId, VIEW_LETTER_PDFS));
            var numberOfLetters = page.getTotalPages();
            return new FetchedLetter(precompiledPdf, numberOfLetters);
        }

    }

    /**
     * "Fetches" letter PDF for sent letter assumed to be uniquely selected by the query parameter
     * values provided. It does so by retrieving the data stored when the letter was sent and using
     * it to regenerate the PDF.
     *
     * @param pscName the name of the PSC intended to receive the letter
     * @param companyNumber the company number
     * @param letterId the ID of the letter
     * @param templateId the ID of the template used to generate the letter
     * @param letterSendingDate the date on which CHIPS triggered the sending of the letter
     * @return the letter PDF
     * @throws IOException should something unexpected happen
     */
    public InputStream fetchLetter(
            final String pscName,
            final String companyNumber,
            final String letterId,
            final String templateId,
            final LocalDate letterSendingDate,
            final String contextId)
            throws IOException {

        var letter = fetchLetterFromDatabase(pscName, companyNumber, letterId, templateId, letterSendingDate);
        var reference = letter.getSenderDetails().getReference();
        var html = getHtml(letter, reference, contextId);

        try (var precompiledPdf = pdfGenerator.generatePdfFromHtml(html, reference)) {
            logger.debug(
                "Responding with regenerated letter PDF to view for letter with "
                + queryParameters(pscName, companyNumber, letterId, templateId, letterSendingDate),
                createLogMap(contextId, VIEW_LETTER_PDF));
            return precompiledPdf;
        }
    }

    /**
     * "Fetches" letter PDFs for sent letters selected by the query parameter values provided,
     * one at a time. It does so by retrieving the data stored when the letter was sent and using
     * it to regenerate the PDF.
     *
     * @param pscName the name of the PSC intended to receive the letter
     * @param companyNumber the company number
     * @param templateId the ID of the template used to generate the letter. This corresponds
     *                   directly to the letter type.
     * @param letterSendingDate the date on which CHIPS triggered the sending of the letter
     * @param letterNumber the number of the specific letter to be fetched, from the collection of
     *                     selected letters, ordered by the createdAt date. The first such letter
     *                     is letter number 1.
     * @return the letter PDF
     * @throws IOException should something unexpected happen
     */
    public FetchedLetter fetchLetter(
            final String pscName,
            final String companyNumber,
            final String letterId,
            final String templateId,
            final LocalDate letterSendingDate,
            final int letterNumber,
            final String contextId)
            throws IOException {

        validateLetterNumber(letterNumber);
        var page = notificationDatabaseService.getLettersByPscNameOrLetterAndCompanyTemplateDate(
                pscName, companyNumber, letterId, templateId, letterSendingDate, letterNumber);
        var letter = getLetter(page, letterNumber);
        var reference = letter.getSenderDetails().getReference();
        var html = getHtml(letter, reference, contextId);

        try (var precompiledPdf = pdfGenerator.generatePdfFromHtml(html, reference)) {
            logger.debug(
                    "Responding with regenerated letter PDF to view for letter with "
                            + queryParameters(pscName,
                                              companyNumber,
                                              letterId,
                                              templateId,
                                              letterSendingDate,
                                              letterNumber),
                    createLogMap(contextId, VIEW_LETTER_PDFS));
            var numberOfLetters = page.getTotalPages();
            return new FetchedLetter(precompiledPdf, numberOfLetters);
        }
    }

    private String getHtml(final GovUkLetterDetailsRequest letter,
                           final String reference,
                           final String contextId) {
        var appId = letter.getSenderDetails().getAppId();
        var letterId = letter.getLetterDetails().getLetterId();
        var templateId = letter.getLetterDetails().getTemplateId();
        var personalisationDetailsString = letter.getLetterDetails().getPersonalisationDetails();
        var personalisationDetails =
                parser.parsePersonalisationDetails(personalisationDetailsString, contextId);
        var address = letter.getRecipientDetails().getPhysicalAddress();
        var originalSendingDate = letter.getCreatedAt();

        personalisationDetails.put(ORIGINAL_SENDING_DATE,
                originalSendingDate.format(DATE_FORMATTER));

        return templatePersonaliser.personaliseLetterTemplate(
                new LetterTemplateKey(
                        appId,
                        letterId,
                        templateId),
                reference,
                personalisationDetails,
                address);
    }

    private GovUkLetterDetailsRequest getLetter(final Page<NotificationLetterRequest> page,
                                                final int letterNumber) {
        page.stream().skip(letterNumber - 1L);
        var request = page.stream().findFirst();
        if (request.isEmpty()) {
            throw new LetterNotFoundException(
                    "Letter number " + letterNumber + " not found. "
                            + "Total number of matching letters was " + page.getTotalPages() + ".");
        }
        return request.get().getRequest();
    }

    private GovUkLetterDetailsRequest fetchLetterFromDatabase(final String pscName,
                                                              final String companyNumber,
                                                              final String letterId,
                                                              final String templateId,
                                                              final LocalDate letterSendingDate) {
        var letters = notificationDatabaseService.getLettersByPscNameOrLetterAndCompanyTemplateDate(
                pscName,
                companyNumber,
                letterId,
                templateId,
                letterSendingDate);
        if (letters.isEmpty()) {
            throw new LetterNotFoundException("Letter not found for "
                    + queryParameters(pscName, companyNumber, letterId, templateId, letterSendingDate));
        } else if (letters.size() > 1) {
            throw new TooManyLettersFoundException("Multiple letters found for "
                    + queryParameters(pscName, companyNumber, letterId, templateId, letterSendingDate));
        }

        return letters.getFirst().getRequest();
    }

    private String queryParameters(final String pscName,
                                   final String companyNumber,
                                   final String letterId,
                                   final String templateId,
                                   final LocalDate letterSendingDate) {
        return "psc name " + pscName
                + ", companyNumber " + companyNumber
                + ", letterId " + letterId
                + ", templateId " + templateId
                + ", letter sending date " + letterSendingDate + ".";
    }

    private String queryParameters(final String pscName,
                                   final String companyNumber,
                                   final String letterId,
                                   final String templateId,
                                   final LocalDate letterSendingDate,
                                   final int letterNumber) {
        return "psc name " + pscName
                + ", companyNumber " + companyNumber
                + ", letterId " + letterId
                + ", templateId " + templateId
                + ", letter sending date " + letterSendingDate
                + ", letter number " + letterNumber + ".";
    }

    private void validateLetterNumber(final int letterNumber) {
        if (letterNumber < 1) {
            throw new LetterValidationException(
                    "Letter number (" + letterNumber + ") cannot be less than 1.");
        }
    }
}
