package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.controller;

import java.util.Optional;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.UserContext;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspMembersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.acsp_manage_users.api.UserAcspMembershipInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

@RestController
public class UserAcspMembershipController implements UserAcspMembershipInterface {

    private final AcspMembersService acspMembersService;

    private static final Logger LOG =
            LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    public UserAcspMembershipController(final AcspMembersService acspMembersService) {
        this.acspMembersService = acspMembersService;
    }

    @Override
    public ResponseEntity<AcspMembershipsList> getAcspMembershipsForUserId(
            final String xRequestId, final String ericIdentity, final Boolean includeRemoved) {

        LOG.infoContext(xRequestId, String.format("Received request with user_id=%s, include_removed=%b", ericIdentity, includeRemoved), null);

        final var loggedUser = Optional.ofNullable(UserContext.getLoggedUser()).orElseThrow(() -> new InternalServerErrorRuntimeException(String.format("User not in context:%s", ericIdentity)));
        LOG.debugContext(xRequestId, String.format("Attempting to fetch memberships for user %s", loggedUser.getUserId()), null);
        final var acspMemberships =
                acspMembersService.fetchAcspMemberships(loggedUser, includeRemoved);

        LOG.infoContext(xRequestId, String.format("Successfully retrieved memberships for user %s", ericIdentity), null);

        return new ResponseEntity<>(acspMemberships, HttpStatus.OK);
    }
}
