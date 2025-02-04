package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model;

import jakarta.validation.constraints.NotNull;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;
import org.springframework.data.mongodb.core.mapping.Field;

import java.time.LocalDateTime;

@Document("acsp_members")
public class AcspMembersDao {

    @Id
    private String id;

    @NotNull
    @Indexed
    @Field("acsp_number")
    private String acspNumber;

    @NotNull
    @Indexed
    @Field("user_id")
    private String userId;

    @NotNull
    @Field("user_role")
    private String userRole;

    @CreatedDate
    @Field("created_at")
    private LocalDateTime createdAt;

    @Field("added_at")
    private LocalDateTime addedAt;

    @Field("added_by")
    private String addedBy;

    @Field("removed_at")
    private LocalDateTime removedAt;

    @Field("removed_by")
    private String removedBy;

    private String status;

    @NotNull
    private String etag;

    @Version
    private Integer version;

    public AcspMembersDao() {
        // Empty constructor
    }

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getAcspNumber() {
        return acspNumber;
    }

    public void setAcspNumber(String acspNumber) {
        this.acspNumber = acspNumber;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getUserRole() {
        return userRole;
    }

    public void setUserRole(String userRole) {
        this.userRole = userRole;
    }

    public LocalDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }

    public LocalDateTime getAddedAt() {
        return addedAt;
    }

    public void setAddedAt(LocalDateTime addedAt) {
        this.addedAt = addedAt;
    }

    public String getAddedBy() {
        return addedBy;
    }

    public void setAddedBy(String addedBy) {
        this.addedBy = addedBy;
    }

    public LocalDateTime getRemovedAt() {
        return removedAt;
    }

    public void setRemovedAt(LocalDateTime removedAt) {
        this.removedAt = removedAt;
    }

    public String getRemovedBy() {
        return removedBy;
    }

    public void setRemovedBy(String removedBy) {
        this.removedBy = removedBy;
    }

    public String getEtag() {
        return etag;
    }

    public void setEtag(String etag) {
        this.etag = etag;
    }

    public Integer getVersion() {
        return version;
    }

    public void setVersion(Integer version) {
        this.version = version;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    @Override
    public String toString() {
        return "AcspMembersDao{" +
                "id='" + id + '\'' +
                ", acspNumber='" + acspNumber + '\'' +
                ", userId='" + userId + '\'' +
                ", userRole=" + userRole +
                ", createdAt=" + createdAt +
                ", addedAt=" + addedAt +
                ", addedBy='" + addedBy + '\'' +
                ", removedAt=" + removedAt +
                ", removedBy='" + removedBy + '\'' +
                ", status=" + status +
                ", etag='" + etag + '\'' +
                ", version=" + version +
                '}';
    }

}
