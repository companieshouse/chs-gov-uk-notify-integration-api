package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.chs.notification.model.GovUkEmailDetailsRequest;

import java.time.LocalDateTime;

@Document(collection = "email_details")
public class NotificationEmailRequest {
    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("request")
    private GovUkEmailDetailsRequest request;

    @Id
    private String id;

    public NotificationEmailRequest(LocalDateTime createdAt, LocalDateTime updatedAt, GovUkEmailDetailsRequest request, String id) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.request = request;
        this.id = id;
    }

    public NotificationEmailRequest() {
    }

    public GovUkEmailDetailsRequest getRequest() {
        return request;
    }

    public String getId() {
        return id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setRequest(GovUkEmailDetailsRequest request) {
        this.request = request;
    }

    public void setId(String id) {
        this.id = id;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    @Override
    public String toString() {
        return "NotificationEmailRequest{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", request=" + request +
                ", id='" + id + '\'' +
                '}';
    }
}

