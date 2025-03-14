package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.lettergovuknotifypayload;

import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.govukconnection.GovUkConnectionInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator.PdfGeneratorInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.TemplateLookupInterface;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation.TemplatePersonalisationInterface;

@Component
public interface LetterGovUkNotifyPayloadInterface {

    TemplateLookupInterface templateLookup = null;
    TemplatePersonalisationInterface templatePersonalisation = null;
    GovUkConnectionInterface govUkConnection = null;
    PdfGeneratorInterface pdfGenerator = null;

}

