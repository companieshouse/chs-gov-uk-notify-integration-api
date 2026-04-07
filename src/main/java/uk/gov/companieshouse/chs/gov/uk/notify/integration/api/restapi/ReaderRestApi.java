package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.restapi;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.ChsGovUkNotifyIntegrationService.APPLICATION_NAMESPACE;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.VIEW_LETTER_PDF;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.api.chs.notification.integration.api.NotifyIntegrationRetrieverControllerInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.sentletterfetcher.SentLetterFetcher;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class ReaderRestApi implements NotifyIntegrationRetrieverControllerInterface {

    private static final String REFERENCE = "reference";
    private static final String LETTER_PDF_IO_ERROR_MESSAGE =
            "Failed to load precompiled letter PDF. Caught IOException: ";

    private static final Logger LOGGER = LoggerFactory.getLogger(APPLICATION_NAMESPACE);
    private final SentLetterFetcher fetcher;

    public ReaderRestApi(final SentLetterFetcher fetcher) {
        this.fetcher = fetcher;
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
