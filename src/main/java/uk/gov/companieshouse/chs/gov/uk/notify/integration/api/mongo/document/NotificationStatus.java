package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.mongodb.config.EnableMongoAuditing;
import org.springframework.data.mongodb.core.mapping.Field;


@EnableMongoAuditing(dateTimeProviderRef = "mongodbDatetimeProvider")
public class NotificationStatus {

    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

     @Field("requestId") String requestId;

     @Field("responseId") String responseId;

     @Field("status") String status;

     @Field("statusDetails") Map<String, Object> statusDetails;

     private String id;

    public NotificationStatus(LocalDateTime createdAt, LocalDateTime updatedAt, String requestId, String responseId, String status, Map<String, Object> statusDetails, String id) {
        this.createdAt = createdAt;
        this.updatedAt = updatedAt;
        this.requestId = requestId;
        this.responseId = responseId;
        this.status = status;
        this.statusDetails = statusDetails;
        this.id = id;
    }

    public NotificationStatus() {
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

    public String getRequestId() {
        return requestId;
    }

    public void setRequestId(String requestId) {
        this.requestId = requestId;
    }

    public String getResponseId() {
        return responseId;
    }

    public void setResponseId(String responseId) {
        this.responseId = responseId;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public Map<String, Object> getStatusDetails() {
        return statusDetails;
    }

    public void setStatusDetails(Map<String, Object> statusDetails) {
        this.statusDetails = statusDetails;
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    @Override
    public String toString() {
        return "NotificationStatus{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", requestId='" + requestId + '\'' +
                ", responseId='" + responseId + '\'' +
                ", status='" + status + '\'' +
                ", statusDetails=" + statusDetails +
                ", Id='" + id + '\'' +
                '}';
    }
}
