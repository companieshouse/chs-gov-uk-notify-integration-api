package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.letterdispatcher;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.Map;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.api.chs.notification.model.Address;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service.NotificationDatabaseService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.HtmlPdfGenerator;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.GovUkNotifyService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.Postage;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.LetterTemplateKey;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonaliser;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.PersonalisationDetailsParser;
import uk.gov.companieshouse.logging.Logger;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
public class LetterDispatcherTest {

    @Mock
    private GovUkNotifyService govUkNotifyService;
    @Mock
    private NotificationDatabaseService notificationDatabaseService;
    @Mock
    private TemplatePersonaliser templatePersonaliser;
    @Mock
    private HtmlPdfGenerator pdfGenerator;
    @Mock
    private PersonalisationDetailsParser parser;
    @Mock
    private Logger logger;

    @InjectMocks
    private LetterDispatcher letterDispatcher;

    @Test
    void testSendLetter_success() throws IOException {
        // Arrange
        Postage postage = Postage.FIRST_CLASS;
        String reference = "ref";
        String appId = "app";
        String letterId = "letter";
        String templateId = "template";
        Address address = new Address();
        String contextId = "ctx";
        String personalisedLetter = "<html>letter</html>";
        String govNotifyReference = appId + "-" + letterId + "-" + reference;
        String personalisationDetails = "{}";
        Map<String, String> personalisationDetailsMap = Collections.emptyMap();

        when(parser.parsePersonalisationDetails(personalisationDetails, contextId))
                .thenReturn(personalisationDetailsMap);
        when(templatePersonaliser.personaliseLetterTemplate(
                new LetterTemplateKey(appId, letterId, templateId), reference,
                personalisationDetailsMap, address)).thenReturn(personalisedLetter);

        InputStream pdfStream = new ByteArrayInputStream(new byte[] {});
        when(pdfGenerator.generatePdfFromHtml(personalisedLetter, govNotifyReference))
                .thenReturn(pdfStream);

        GovUkNotifyService.LetterResp letterResp = mock(GovUkNotifyService.LetterResp.class);
        when(govUkNotifyService.sendLetter(postage, govNotifyReference, pdfStream))
                .thenReturn(letterResp);

        // Act
        GovUkNotifyService.LetterResp result = letterDispatcher.sendLetter(postage, reference,
                appId, letterId, templateId, address, personalisationDetails, contextId);

        // Assert
        assertEquals(letterResp, result);
        verify(notificationDatabaseService).storeResponse(letterResp);
    }

    @Test
    void testSendOldLetter_success() throws IOException {
        // Arrange
        Postage postage = Postage.ECONOMY;
        String reference = "ref";
        String appId = "app";
        String letterId = null; // An old letter does not have a letterId
        String templateId = "template";
        Address address = new Address();
        String contextId = "ctx";
        String personalisedLetter = "<html>letter</html>";
        String govNotifyReference = reference;
        String personalisationDetails = "{}";
        Map<String, String> personalisationDetailsMap = Collections.emptyMap();

        when(parser.parsePersonalisationDetails(personalisationDetails, contextId))
                .thenReturn(personalisationDetailsMap);
        when(templatePersonaliser.personaliseLetterTemplate(
                new LetterTemplateKey(appId, letterId, templateId), reference,
                personalisationDetailsMap, address)).thenReturn(personalisedLetter);

        InputStream pdfStream = new ByteArrayInputStream(new byte[] {});
        when(pdfGenerator.generatePdfFromHtml(personalisedLetter, govNotifyReference))
                .thenReturn(pdfStream);

        GovUkNotifyService.LetterResp letterResp = mock(GovUkNotifyService.LetterResp.class);
        when(govUkNotifyService.sendLetter(postage, govNotifyReference, pdfStream))
                .thenReturn(letterResp);

        // Act
        GovUkNotifyService.LetterResp result = letterDispatcher.sendLetter(postage, reference,
                appId, letterId, templateId, address, personalisationDetails, contextId);

        // Assert
        assertEquals(letterResp, result);
        verify(notificationDatabaseService).storeResponse(letterResp);
    }
}
