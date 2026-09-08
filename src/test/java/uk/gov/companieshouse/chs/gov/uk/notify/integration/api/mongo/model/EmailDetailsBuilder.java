package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import static org.apache.commons.lang3.RandomStringUtils.secure;

import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

public class EmailDetailsBuilder {

    private String templateId = UUID.randomUUID().toString();
    private String attachmentId;
    private HashMap<String, Object> personalisationDetails = new HashMap<>(Map.of("companyNumber", secure().nextNumeric(8)));

    public static EmailDetailsBuilder emailDetailsBuilder() {
        return new EmailDetailsBuilder();
    }

    public EmailDetailsDao build() {
        EmailDetailsDao emailDetailsDao = new EmailDetailsDao();
        emailDetailsDao.setTemplateId(templateId);
        emailDetailsDao.setAttachmentId(attachmentId);
        emailDetailsDao.setPersonalisationDetails(personalisationDetails);
        return emailDetailsDao;
    }

    public EmailDetailsBuilder withTemplateId(String templateId) {
        this.templateId = templateId;
        return this;
    }

    public EmailDetailsBuilder withPersonalisationDetails(Map<String, Object> personalisationDetails) {
        this.personalisationDetails = Objects.nonNull(personalisationDetails) ? new HashMap<>(personalisationDetails) : null;
        return this;
    }

    public EmailDetailsBuilder withAttachmentId(String attachmentId) {
        this.attachmentId = attachmentId;
        return this;
    }
}
