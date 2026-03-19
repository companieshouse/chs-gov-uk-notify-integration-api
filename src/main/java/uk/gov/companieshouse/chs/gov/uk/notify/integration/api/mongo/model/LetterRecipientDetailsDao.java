package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.util.Objects;
import org.springframework.data.mongodb.core.mapping.Field;

public class LetterRecipientDetailsDao {
    @Field("name")
    private String name;

    @Field("physical_address")
    private AddressDao physicalAddress;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }

    public AddressDao getPhysicalAddress() {
        return physicalAddress;
    }

    public void setPhysicalAddress(AddressDao physicalAddress) {
        this.physicalAddress = physicalAddress;
    }

    @Override
    public int hashCode() {
        return Objects.hash(name, physicalAddress);
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
        LetterRecipientDetailsDao other = (LetterRecipientDetailsDao) obj;
        return Objects.equals(name, other.name)
                && Objects.equals(physicalAddress, other.physicalAddress);
    }

}