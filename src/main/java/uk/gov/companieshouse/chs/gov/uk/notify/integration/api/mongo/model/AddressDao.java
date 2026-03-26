package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mongo.model;

import java.util.Objects;
import org.springframework.data.mongodb.core.mapping.Field;

public class AddressDao {
    @Field("address_line_1")
    private String addressLine1;

    @Field("address_line_2")
    private String addressLine2;

    @Field("address_line_3")
    private String addressLine3;

    @Field("address_line_4")
    private String addressLine4;

    @Field("address_line_5")
    private String addressLine5;

    @Field("address_line_6")
    private String addressLine6;

    @Field("address_line_7")
    private String addressLine7;

    public String getAddressLine1() {
        return addressLine1;
    }

    public void setAddressLine1(String addressLine1) {
        this.addressLine1 = addressLine1;
    }

    public String getAddressLine2() {
        return addressLine2;
    }

    public void setAddressLine2(String addressLine2) {
        this.addressLine2 = addressLine2;
    }

    public String getAddressLine3() {
        return addressLine3;
    }

    public void setAddressLine3(String addressLine3) {
        this.addressLine3 = addressLine3;
    }

    public String getAddressLine4() {
        return addressLine4;
    }

    public void setAddressLine4(String addressLine4) {
        this.addressLine4 = addressLine4;
    }

    public String getAddressLine5() {
        return addressLine5;
    }

    public void setAddressLine5(String addressLine5) {
        this.addressLine5 = addressLine5;
    }

    public String getAddressLine6() {
        return addressLine6;
    }

    public void setAddressLine6(String addressLine6) {
        this.addressLine6 = addressLine6;
    }

    public String getAddressLine7() {
        return addressLine7;
    }

    public void setAddressLine7(String addressLine7) {
        this.addressLine7 = addressLine7;
    }

    @Override
    public int hashCode() {
        return Objects.hash(addressLine1, addressLine2, addressLine3, addressLine4, addressLine5,
                addressLine6, addressLine7);
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
        AddressDao other = (AddressDao) obj;
        return Objects.equals(addressLine1, other.addressLine1)
                && Objects.equals(addressLine2, other.addressLine2)
                && Objects.equals(addressLine3, other.addressLine3)
                && Objects.equals(addressLine4, other.addressLine4)
                && Objects.equals(addressLine5, other.addressLine5)
                && Objects.equals(addressLine6, other.addressLine6)
                && Objects.equals(addressLine7, other.addressLine7);
    }

}