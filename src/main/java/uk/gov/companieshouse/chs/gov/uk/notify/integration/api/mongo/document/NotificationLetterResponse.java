package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;
import uk.gov.service.notify.LetterResponse;

import java.time.LocalDateTime;


@EnableMongoAuditing(dateTimeProviderRef = "mongodbDatetimeProvider")
public class NotificationLetterResponse {

    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

    private LetterResponse response;

    private String Id;

    public NotificationLetterResponse(LocalDateTime createdAt, LocalDateTime updatedAt, LetterResponse response, String id) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.response = response;
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

    public LetterResponse getResponse() {
        return response;
    }

    public void setResponse(LetterResponse response) {
        this.response = response;
    }

    public String getId() {
        return Id;
    }

    public void setId(String id) {
        Id = id;
    }

    @Override
    public String toString() {
        return "NotificationLetterResponse{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", response=" + response +
                ", Id='" + Id + '\'' +
                '}';
    }
}

