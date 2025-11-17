package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.document;

import java.time.LocalDateTime;
import java.util.Map;

import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

@Document(collection = "status")
public class NotificationStatus {

    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("requestId")
    private String requestId;

    @Field("responseId")
    private String responseId;

    @Field("status")
    private String status;

    @Field("statusDetails")
    private Map<String, Object> statusDetails;

    @Id
    private String id;

    @Version
    private Integer version;

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

    // NOSONAR
    public int getVersion(){
        return version;
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
                ", version=" + version +
                ", Id='" + id + '\'' +
                '}';
    }
}
