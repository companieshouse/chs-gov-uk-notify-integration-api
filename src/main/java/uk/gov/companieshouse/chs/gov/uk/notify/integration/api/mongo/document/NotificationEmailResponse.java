package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.service.notify.SendEmailResponse;

import java.time.LocalDateTime;

@Document(collection = "responses")
public class NotificationEmailResponse {

    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("response")
    private SendEmailResponse response;

    @Id
    private String id;

    @Version
    private Integer version;

    public NotificationEmailResponse(LocalDateTime createdAt, LocalDateTime updatedAt, SendEmailResponse response, String id) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.response = response;
        this.id = id;
    }

    public NotificationEmailResponse() {
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

    public SendEmailResponse getResponse() {
        return response;
    }

    public void setResponse(SendEmailResponse response) {
        this.response = response;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NotificationEmailResponse{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", response=" + response +
                ", id='" + id + '\'' +
                ", version=" + version +
                '}';
    }
}

