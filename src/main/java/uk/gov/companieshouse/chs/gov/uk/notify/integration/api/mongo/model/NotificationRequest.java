package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.time.LocalDateTime;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Field;

public abstract class NotificationRequest<T> {

    @Id
    private String id;

    @Field("created_at") @CreatedDate
    private LocalDateTime createdAt;

    @Field("updated_at") @LastModifiedDate
    private LocalDateTime updatedAt;

    @Field("request")
    private T request;

    @Field("status")
    private RequestStatus status;

    @Version
    private Integer version;

    public T getRequest() {
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

    public void setRequest(T request) {
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

    public RequestStatus getStatus() {
        return status;
    }

    public void setStatus(RequestStatus status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "{" +
                "createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                ", request=" + request +
                ", status=" + status +
                ", id='" + id + '\'' +
                ", version=" + version +
                '}';
    }
}
