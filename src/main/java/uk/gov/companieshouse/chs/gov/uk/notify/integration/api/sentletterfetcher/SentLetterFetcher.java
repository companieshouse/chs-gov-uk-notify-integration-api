package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.sentletterfetcher;

import java.io.InputStream;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.LetterNotFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.TooManyLettersFoundException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;

/**
 * "Fetches" letter PDF for sent letter assumed to be uniquely identified by the reference.
 * It does so by retrieving the data stored when the letter was sent and using it to regenerate
 * the PDF.
 */
@Component
public class SentLetterFetcher {

    private final NotificationDatabaseService notificationDatabaseService;

    public SentLetterFetcher(final NotificationDatabaseService notificationDatabaseService) {
        this.notificationDatabaseService = notificationDatabaseService;
    }

    public InputStream fetchLetter(final String reference) {

        // TODO DEEP-428 Tidy this up?
        var letters = notificationDatabaseService.getLetterByReference(reference);
        if (letters.isEmpty()) {
            throw new LetterNotFoundException("Letter not found for reference: " + reference);
        } else if (letters.size() > 1) {
            throw new TooManyLettersFoundException("Multiple letters found for reference: "
                    + reference);
        }
        var letter = letters.getFirst();

        // TODO DEEP-428 Replace this PDF with one regenerated from retrieved letter data.
        return getClass().getResourceAsStream("/letter.pdf");
    }
}
