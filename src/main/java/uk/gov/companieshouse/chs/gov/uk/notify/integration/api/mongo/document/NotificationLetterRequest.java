package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.companieshouse.api.chs.notification.model.GovUkLetterDetailsRequest;

import java.time.LocalDateTime;

@Document(collection = "letter_details")
public class NotificationLetterRequest {

    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("request")
    private GovUkLetterDetailsRequest request;

    @Id
    private String id;

    public NotificationLetterRequest(LocalDateTime createdAt, LocalDateTime updatedAt, GovUkLetterDetailsRequest request, String id) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.request = request;
        this.id = id;
    }

    public NotificationLetterRequest() {
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
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NotificationLetterRequest{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", request=" + request +
                ", Id='" + id + '\'' +
                '}';
    }
}
