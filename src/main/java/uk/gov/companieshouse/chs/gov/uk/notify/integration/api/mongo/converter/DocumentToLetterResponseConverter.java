package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.converter;

import java.util.UUID;

import org.bson.Document;
import org.json.JSONObject;
import org.springframework.core.convert.converter.Converter;
import org.springframework.data.convert.ReadingConverter;
import uk.gov.service.notify.LetterResponse;

@ReadingConverter
public class DocumentToLetterResponseConverter implements Converter<Document, LetterResponse> {

    public static final String REFERENCE = "reference";
    private static final String POSTAGE = "postage";

    @Override
    public LetterResponse convert(Document source) {
        JSONObject json = new JSONObject();

        UUID notificationId = source.get("notificationId", UUID.class);
        json.put("id", notificationId.toString());

        if (source.containsKey(REFERENCE)) {
            json.put(REFERENCE, source.getString(REFERENCE));
        }

        if (source.containsKey(POSTAGE)) {
            json.put(POSTAGE, source.getString(POSTAGE));
        }

        return new LetterResponse(json.toString());
    }
}
