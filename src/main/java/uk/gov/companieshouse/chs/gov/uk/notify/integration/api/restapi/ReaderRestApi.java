package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static java.time.format.DateTimeFormatter.ISO_DATE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.VIEW_LETTER_PDF;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.VIEW_LETTER_PDFS;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import java.io.IOException;
import java.time.LocalDate;
import org.apache.commons.io.IOUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationRetrieverControllerInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.sentletterfetcher.SentLetterFetcher;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.validation.ViewLetterValidator;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class ReaderRestApi implements NotifyIntegrationRetrieverControllerInterface {

    private static final String REFERENCE = "reference";
    private static final String LETTER_PDF_IO_ERROR_MESSAGE =
            "Failed to load precompiled letter PDF. Caught IOException: ";
    // Filename element separator. Constant name made short for intelligibility of code composing
    // filenames.
    private static final String SEP = "-";
    private static final String LETTER_ID = "letter_id";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private final SentLetterFetcher fetcher;
    private final ViewLetterValidator validator;

    public ReaderRestApi(final SentLetterFetcher fetcher,
                         final ViewLetterValidator validator) {
        this.fetcher = fetcher;
        this.validator = validator;
    }

    @Override
    public ResponseEntity<Object> viewLetterPdfByReference(
            final String reference,
            final String contextId) {

        var logMap = createLogMap(contextId, VIEW_LETTER_PDF);
        logMap.put(REFERENCE, reference);
        LOGGER.info("Starting viewLetterPdfByReference process", logMap);

        try {
            return ResponseEntity
                    .ok()
                    .headers(suggestFilename(reference))
                    .body(IOUtils.toByteArray(fetcher.fetchLetter(reference, contextId)));
        } catch (IOException ioe) {
            LOGGER.error(LETTER_PDF_IO_ERROR_MESSAGE + ioe.getMessage(),
                    createLogMap(contextId, "load_pdf_error"));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Object> viewLetterPdfsByReference(
            final String reference,
            final Integer letterNumber,
            final String contextId) {

        var logMap = createLogMap(contextId, VIEW_LETTER_PDFS);
        logMap.put(REFERENCE, reference);
        logMap.put("letter", letterNumber);
        LOGGER.info("Starting viewLetterPdfsByReference process", logMap);

        try {
            var fetchedLetter = fetcher.fetchLetter(reference, letterNumber, contextId);
            var fileName =
                    reference + "_" + letterNumber + "_of_" + fetchedLetter.numberOfLetters();
            return ResponseEntity
                    .ok()
                    .headers(suggestFilename(fileName))
                    .body(IOUtils.toByteArray(fetchedLetter.letter()));
        } catch (IOException ioe) {
            LOGGER.error(LETTER_PDF_IO_ERROR_MESSAGE + ioe.getMessage(),
                    createLogMap(contextId, "load_pdf_error"));
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Object> viewLetterPdf(
            final String companyNumber,
            final String templateId,
            final LocalDate letterSendingDate,
            final String contextId,
            final String pscName,
            final String letterId) {

        var logMap = createLogMap(contextId, VIEW_LETTER_PDF);
        logMap.put("psc_name", pscName);
        logMap.put("company_number", companyNumber);
        logMap.put(LETTER_ID, letterId);
        logMap.put("template_id", templateId);
        logMap.put("letter_sending_date", letterSendingDate.format(ISO_DATE));
        LOGGER.info("Starting viewLetterPdf process", logMap);

        validator.validateViewLetterInputs(pscName, letterId);

        try {
            return ResponseEntity
                .ok()
                .headers(suggestFilename(pscName + SEP + templateId + SEP + letterSendingDate))
                .body(IOUtils.toByteArray(
                        fetcher.fetchLetter(
                                pscName,
                                companyNumber,
                                letterId,
                                templateId,
                                letterSendingDate,
                                contextId)));
        } catch (IOException ioe) {
            LOGGER.error(LETTER_PDF_IO_ERROR_MESSAGE + ioe.getMessage(), logMap);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    @Override
    public ResponseEntity<Object> viewLetterPdfs(
            final String companyNumber,
            final String templateId,
            final LocalDate letterSendingDate,
            final Integer letterNumber,
            final String contextId,
            final String pscName,
            final String letterId) {

        var logMap = createLogMap(contextId, VIEW_LETTER_PDFS);
        logMap.put("psc_name", pscName);
        logMap.put("company_number", companyNumber);
        logMap.put(LETTER_ID, letterId);
        logMap.put("template_id", templateId);
        logMap.put("letter_sending_date", letterSendingDate.format(ISO_DATE));
        logMap.put("letter", letterNumber);
        LOGGER.info("Starting viewLetterPdfs process", logMap);

        validator.validateViewLetterInputs(pscName, letterId);

        try {
            var fetchedLetter = fetcher.fetchLetter(
                    pscName,
                    companyNumber,
                    letterId,
                    templateId,
                    letterSendingDate,
                    letterNumber,
                    contextId);
            var fileName = pscName + SEP + templateId + SEP + letterSendingDate
                    + "_" + letterNumber + "_of_" + fetchedLetter.numberOfLetters();
            return ResponseEntity
                    .ok()
                    .headers(suggestFilename(fileName))
                    .body(IOUtils.toByteArray(fetchedLetter.letter()));
        } catch (IOException ioe) {
            LOGGER.error(LETTER_PDF_IO_ERROR_MESSAGE + ioe.getMessage(), logMap);
            return new ResponseEntity<>(HttpStatus.INTERNAL_SERVER_ERROR);
        }
    }

    /**
     * Suggests that the downloaded file be saved to the filename provided with '.pdf' as
     * the filename suffix.
     * @param filename the suggested filename
     * @return headers to be set in the response
     */
    private HttpHeaders suggestFilename(final String filename) {
        var contentDisposition = ContentDisposition.inline().filename(filename + ".pdf").build();
        var headers = new HttpHeaders();
        headers.setContentDisposition(contentDisposition);
        return headers;
    }

}
