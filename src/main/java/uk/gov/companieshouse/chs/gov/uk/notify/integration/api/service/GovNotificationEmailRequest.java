package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import java.util.Map;

public record GovNotificationEmailRequest(
        String emailAddress,
        String templateId,
        String reference,
        Map<String, Object> personalisationDetails,
        AttachmentFile attachment) {
}
