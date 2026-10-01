package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

public class NotificationEmailRequestBuilder {

    private RequestStatus status;
    private EmailRequestDao request;

    public static NotificationEmailRequestBuilder notificationEmailRequestBuilder() {
        return new NotificationEmailRequestBuilder();
    }

    public NotificationEmailRequest build() {
        NotificationEmailRequest notificationEmailRequest = new NotificationEmailRequest();
        notificationEmailRequest.setRequest(request);
        notificationEmailRequest.setStatus(status);
        return notificationEmailRequest;
    }

    public NotificationEmailRequestBuilder withStatus(RequestStatus status) {
        this.status = status;
        return this;
    }

    public NotificationEmailRequestBuilder withRequest(EmailRequestDao request) {
        this.request = request;
        return this;
    }
}
