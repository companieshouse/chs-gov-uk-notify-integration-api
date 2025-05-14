package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDateTime;

@EnableMongoAuditing(dateTimeProviderRef = "mongodbDatetimeProvider")
public class NotificationLetterRequest {

    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

    private GovUkLetterDetailsRequest request;

    private String Id;

    public NotificationLetterRequest(LocalDateTime createdAt, LocalDateTime updatedAt, GovUkLetterDetailsRequest request, String id) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.request = request;
        Id = id;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }

    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }

    public GovUkLetterDetailsRequest getRequest() {
        return request;
    }

    public void setRequest(GovUkLetterDetailsRequest request) {
        this.request = request;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    @Override
    public String toString() {
        return "NotificationLetterRequest{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", request=" + request +
                ", Id='" + Id + '\'' +
                '}';
    }
}
