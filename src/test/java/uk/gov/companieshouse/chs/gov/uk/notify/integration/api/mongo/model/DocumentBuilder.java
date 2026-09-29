package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import static java.util.Map.entry;

import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.bson.Document;

/**
 * A builder for creating {@link Document} instances with specified key-value pairs.
 */
public class DocumentBuilder {

    Set<Map.Entry<String, Object>> entries = new HashSet<>();

    public static DocumentBuilder documentBuilder() {
        return new DocumentBuilder();
    }

    public Document build() {
        Document document = new Document();
        entries.forEach(entry -> document.put(entry.getKey(), entry.getValue()));
        return document;
    }

    public DocumentBuilder withEntry(String key, Object value) {
        entries.add(entry(key, value));
        return this;
    }

}
