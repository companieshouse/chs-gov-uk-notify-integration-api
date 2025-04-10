package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter;

import java.util.UUID;

import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import uk.gov.service.notify.SendEmailResponse;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter.DocumentToLetterResponseConverter.REFERENCE;

@ReadingConverter
public class DocumentToSendEmailResponseConverter implements Converter<Document, SendEmailResponse> {
    @Override
    public SendEmailResponse convert(Document source) {
        var json = new JSONObject();

        UUID notificationId = source.get("notificationId", UUID.class);
        json.put("id", notificationId.toString());

        if (source.containsKey(REFERENCE)) {
            json.put(REFERENCE, source.getString(REFERENCE));
        }

        var contentJson = new JSONObject();
        contentJson.put("body", source.getString("body"));
        contentJson.put("subject", source.getString("subject"));
        if (source.containsKey("fromEmail")) {
            contentJson.put("from_email", source.getString("fromEmail"));
        }
        json.put("content", contentJson);

        var templateJson = new JSONObject();
        UUID templateId = source.get("templateId", UUID.class);
        templateJson.put("id", templateId.toString());
        templateJson.put("version", source.getInteger("templateVersion"));
        templateJson.put("uri", source.getString("templateUri"));
        json.put("template", templateJson);

        if (source.containsKey("oneClickUnsubscribeURL")) {
            json.put("one_click_unsubscribe_url", source.get("oneClickUnsubscribeURL").toString());
        }

        return new SendEmailResponse(json.toString());
    }
}
