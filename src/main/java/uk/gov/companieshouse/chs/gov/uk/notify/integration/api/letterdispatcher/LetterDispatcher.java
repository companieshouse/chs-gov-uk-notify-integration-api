package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.LoggingUtils.createLogMap;

import java.io.IOException;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonaliser;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.PersonalisationDetailsParser;
import uk.gov.companieshouse.logging.Logger;

/**
 * Responsible for the creation and sending of letter PDFs through the Gov Notify service.
 */
@Component
public class LetterDispatcher {

    private final GovUkNotifyService govUkNotifyService;
    private final NotificationDatabaseService notificationDatabaseService;
    private final TemplatePersonaliser templatePersonaliser;
    private final HtmlPdfGenerator pdfGenerator;
    private final PersonalisationDetailsParser parser;
    private final Logger logger;

    public LetterDispatcher(GovUkNotifyService govUkNotifyService,
                            NotificationDatabaseService notificationDatabaseService,
                            TemplatePersonaliser templatePersonaliser,
                            HtmlPdfGenerator pdfGenerator,
                            PersonalisationDetailsParser parser,
                            Logger logger) {
        this.govUkNotifyService = govUkNotifyService;
        this.notificationDatabaseService = notificationDatabaseService;
        this.templatePersonaliser = templatePersonaliser;
        this.pdfGenerator = pdfGenerator;
        this.parser = parser;
        this.logger = logger;
    }

    public GovUkNotifyService.LetterResp sendLetter(
            final String postage,
            final String reference,
            final String appId,
            final String templateId,
            final Address address,
            final String personalisationDetailsString,
            final String contextId) throws IOException {
        var letter = personaliseLetter(
                reference,
                appId,
                templateId,
                address,
                personalisationDetailsString,
                contextId);
        return sendLetterPdf(postage, reference, contextId, letter);
    }

    private String personaliseLetter(
            final String reference,
            final String appId,
            final String templateId,
            final Address address,
            final String personalisationDetailsString,
            final String contextId) {

        var personalisationDetails =
                parser.parsePersonalisationDetails(personalisationDetailsString, contextId);

        return templatePersonaliser.personaliseLetterTemplate(
                new LetterTemplateKey(
                        appId,
                        templateId),
                        reference,
                        personalisationDetails,
                        address);
    }

    private GovUkNotifyService.LetterResp
            sendLetterPdf(
                        final String postage,
                        final String reference,
                        final String contextId,
                        final String letter) throws IOException {

        try (var precompiledPdf = pdfGenerator.generatePdfFromHtml(letter, reference)) {

            var response =
                    govUkNotifyService.sendLetter(
                            postage,
                            reference,
                            precompiledPdf);

            logger.debug("Storing letter response in database",
                    createLogMap(contextId, "store_letter_response"));
            notificationDatabaseService.storeResponse(response);

            return response;
        }

    }

}
