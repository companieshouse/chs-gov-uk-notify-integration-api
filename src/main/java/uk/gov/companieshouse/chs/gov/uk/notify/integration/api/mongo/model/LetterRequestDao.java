package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import org.springframework.data.mongodb.core.mapping.Field;
import java.time.OffsetDateTime;
import java.util.Objects;

public class LetterRequestDao {
    @Field("sender_details")
    private SenderDetailsDao senderDetails;

    @Field("recipient_details")
    private LetterRecipientDetailsDao recipientDetails;

    @Field("letter_details")
    private LetterDetailsDao letterDetails;

    @Field("created_at")
    private OffsetDateTime createdAt;

    public SenderDetailsDao getSenderDetails() {
        return senderDetails;
    }

    public void setSenderDetails(SenderDetailsDao senderDetails) {
        this.senderDetails = senderDetails;
    }

    public LetterRecipientDetailsDao getRecipientDetails() {
        return recipientDetails;
    }

    public void setRecipientDetails(LetterRecipientDetailsDao recipientDetails) {
        this.recipientDetails = recipientDetails;
    }

    public LetterDetailsDao getLetterDetails() {
        return letterDetails;
    }

    public void setLetterDetails(LetterDetailsDao letterDetails) {
        this.letterDetails = letterDetails;
    }

    public OffsetDateTime getCreatedAt() {
        return createdAt;
    }

    public void setCreatedAt(OffsetDateTime createdAt) {
        this.createdAt = createdAt;
    }

    @Override
    public int hashCode() {
        return Objects.hash(createdAt, letterDetails, recipientDetails, senderDetails);
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        LetterRequestDao other = (LetterRequestDao) obj;
        return Objects.equals(createdAt, other.createdAt)
                && Objects.equals(letterDetails, other.letterDetails)
                && Objects.equals(recipientDetails, other.recipientDetails)
                && Objects.equals(senderDetails, other.senderDetails);
    }

}
