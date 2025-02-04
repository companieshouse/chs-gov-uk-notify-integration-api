package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mapper;

import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership.AcspStatusEnum;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;

import java.time.LocalDateTime;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import java.util.Objects;

@Mapper(componentModel = "spring")
public abstract class AcspMembershipMapper {

    @Autowired
    protected UsersService usersService;

    @Autowired
    protected AcspProfileService acspProfileService;

    private static final String DEFAULT_DISPLAY_NAME = "Not Provided";

    protected OffsetDateTime localDateTimeToOffsetDateTime(final LocalDateTime localDateTime) {
        return Objects.isNull(localDateTime) ? null : OffsetDateTime.of(localDateTime, ZoneOffset.UTC);
    }

    @AfterMapping
    protected void enrichWithUserDetails(@MappingTarget final AcspMembership acspMembership, @Context User userDetails) {
        if (Objects.isNull(userDetails)) {
            userDetails = usersService.fetchUserDetails(acspMembership.getUserId());
        }
        acspMembership.setUserEmail(userDetails.getEmail());
        acspMembership.setUserDisplayName(Objects.isNull(userDetails.getDisplayName()) ? DEFAULT_DISPLAY_NAME : userDetails.getDisplayName());
    }

    @AfterMapping
    protected void enrichWithAcspProfile(@MappingTarget final AcspMembership acspMembership, @Context AcspProfile acspProfile) {
        if (Objects.isNull(acspProfile)) {
            acspProfile = acspProfileService.fetchAcspProfile(acspMembership.getAcspNumber());
        }
        acspMembership.setAcspName(acspProfile.getName());
        acspMembership.setAcspStatus(AcspStatusEnum.fromValue(acspProfile.getStatus().getValue()));
    }

    @Mapping(target = "userRole", expression = "java(AcspMembership.UserRoleEnum.fromValue(acspMembersDao.getUserRole()))")
    @Mapping(target = "membershipStatus", expression = "java(AcspMembership.MembershipStatusEnum.fromValue(acspMembersDao.getStatus()))")
    public abstract AcspMembership daoToDto(final AcspMembersDao acspMembersDao, @Context final User user, @Context final AcspProfile acspProfile);

}