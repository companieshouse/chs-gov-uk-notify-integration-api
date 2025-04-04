package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.service;

import java.util.List;
import java.util.Optional;

import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkEmailDetailsRequest;
import uk.gov.companieshouse.api.chs_gov_uk_notify_integration_api.model.GovUkLetterDetailsRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationEmailRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationLetterRequest;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document.NotificationStatus;


public interface MongoDataStoreInterface {

    // emails
    NotificationEmailRequest storeEmail(GovUkEmailDetailsRequest emailDetailsRequest);

    Optional<NotificationEmailRequest> getEmail(String id);

    List<NotificationEmailRequest> findAllEmails();

    // letters
    Optional<NotificationLetterRequest> getLetter(String id);

    NotificationLetterRequest storeLetter(GovUkLetterDetailsRequest letterDetails);

    List<NotificationLetterRequest> findAllLetters();

    // shared
    NotificationStatus updateStatus(NotificationStatus notificationStatus);
    
    
}
