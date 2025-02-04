package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.email;

import uk.gov.companieshouse.email_producer.model.EmailData;

public abstract class BaseEmailData<T extends BaseEmailData<T>> extends EmailData {

    protected abstract T self();

    public T to(final String to) {
        setTo(to);
        return self();
    }

    public T subject(final String subject) {
        setSubject(subject);
        return self();
    }

    public abstract void setSubject();

    public T subject() {
        setSubject();
        return self();
    }

    public abstract String toNotificationSentLoggingMessage();

    public abstract String toNotificationSendingFailureLoggingMessage();

}
