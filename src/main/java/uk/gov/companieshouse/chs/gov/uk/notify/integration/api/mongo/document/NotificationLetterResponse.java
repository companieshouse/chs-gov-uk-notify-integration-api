package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;
import uk.gov.service.notify.LetterResponse;

import java.time.LocalDateTime;

@Document(collection = "responses")
public class NotificationLetterResponse {

    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("response")
    private LetterResponse response;

    @Id
    private String id;

    @Version
    private Integer version;

    public NotificationLetterResponse(LocalDateTime createdAt, LocalDateTime updatedAt, LetterResponse response, String id) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.response = response;
        this.id = id;
    }

    public NotificationLetterResponse() {
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
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public int getVersion(){
        return version;
    }

    @Override
    public String toString() {
        return "NotificationLetterResponse{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", response=" + response +
                ", version=" + version +
                ", Id='" + id + '\'' +
                '}';
    }
}

