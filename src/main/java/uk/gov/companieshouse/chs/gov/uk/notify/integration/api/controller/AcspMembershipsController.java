package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.UserContext;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspMembersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.AcspProfileService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.EmailService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service.UsersService;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.PaginationValidatorUtil;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.StaticPropertyUtil;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.api.AcspMembershipsInterface;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyLookup;
import uk.gov.companieshouse.api.acsp_manage_users.model.RequestBodyPost;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import java.util.Arrays;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ErrorCode.ERROR_CODE_1001;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.ErrorCode.ERROR_CODE_1002;
import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.RequestContextUtil.*;

@Controller
public class AcspMembershipsController implements AcspMembershipsInterface {

    private static final Logger LOG = LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);

    private static final String PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN = "Please check the request and try again";
    private static final String ACSP_NUMBER_KEY = "acspNumber";
    private static final String REQUEST_ID_KEY = "requestId";

    private final UsersService usersService;
    private final AcspProfileService acspProfileService;
    private final AcspMembersService acspMembersService;
    private final EmailService emailService;

    public AcspMembershipsController(final UsersService usersService, final AcspProfileService acspProfileService, final AcspMembersService acspMembersService, final EmailService emailService) {
        this.usersService = usersService;
        this.acspProfileService = acspProfileService;
        this.acspMembersService = acspMembersService;
        this.emailService = emailService;
    }

    @Override
    public ResponseEntity<AcspMembership> addMemberForAcsp(final String xRequestId, final String acspNumber, final RequestBodyPost requestBodyPost) {

        final var targetUserId = requestBodyPost.getUserId();
        final var targetUserRole = AcspMembership.UserRoleEnum.fromValue(requestBodyPost.getUserRole().getValue());

        LOG.infoContext(xRequestId, String.format("Received request with acsp_number=%s, user_id=%s, user_role=%s ", acspNumber, targetUserId, targetUserRole.getValue()), null);

        User targetUser;
        try {
            targetUser = usersService.fetchUserDetails(targetUserId);
        } catch (NotFoundRuntimeException exception) {
            throw new BadRequestRuntimeException(ERROR_CODE_1001.getCode());
        }

        AcspProfile acspProfile;
        try {
            acspProfile = acspProfileService.fetchAcspProfile(acspNumber);
        } catch (NotFoundRuntimeException exception) {
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        LOG.debugContext(xRequestId, String.format("Attempting to fetch memberships for user with id %s", targetUserId), null);
        final var memberships = acspMembersService.fetchAcspMembershipDaos(targetUserId, false);
        if (!memberships.isEmpty()) {
            LOG.errorContext(xRequestId, new Exception(String.format("%s user already has an active Acsp membership", targetUserId)), null);
            throw new BadRequestRuntimeException(ERROR_CODE_1002.getCode());
        }

        final var requestingUser = UserContext.getLoggedUser();
        final var requestingUserId = Optional.ofNullable(requestingUser).map(User::getUserId).orElse(null);
        if (isOAuth2Request() && (!requestingUserIsActiveMemberOfAcsp(acspNumber) || !requestingUserIsPermittedToCreateMembershipWith(targetUserRole))) {
            LOG.errorContext(xRequestId, new Exception(String.format("User %s is not permitted to create %s membership", requestingUserId, targetUserRole.getValue())), null);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        LOG.debugContext(xRequestId, String.format("Attempting to create membership for user %s and Acsp %s", targetUserId, acspNumber), null);
        final var membership = acspMembersService.addAcspMembership(targetUser, acspProfile, acspNumber, targetUserRole, requestingUserId);

        if (isOAuth2Request()) {
            final var requestingUserDisplayName = Optional.ofNullable(requestingUser.getDisplayName()).orElse(requestingUser.getEmail());
            emailService.sendConfirmYouAreAMemberEmail(xRequestId, targetUser.getEmail(), requestingUserDisplayName, acspProfile.getName(), targetUserRole);
        }

        LOG.infoContext(xRequestId, String.format("Successfully created %s membership for user %s at Acsp %s", targetUserRole.getValue(), targetUserId, acspNumber), null);

        return new ResponseEntity<>(membership, HttpStatus.CREATED);
    }


    @Override
    public ResponseEntity<AcspMembershipsList> findMembershipsForUserAndAcsp(
            final String requestId,
            final String acspNumber,
            final Boolean includeRemoved,
            final RequestBodyLookup requestBody) {

        if (Objects.isNull(requestBody.getUserEmail())) {
            LOG.errorContext(requestId, new Exception("User email was not provided."), null);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }
        final var userEmail = requestBody.getUserEmail();

        LOG.infoContext(requestId, String.format("Received request with acsp_number=%s, include_removed=%s, user_email=%s", acspNumber, includeRemoved, userEmail), null);

        final var usersList =
                Optional.ofNullable(usersService.searchUserDetails(List.of(userEmail)))
                        .filter(users -> !users.isEmpty())
                        .orElseThrow(
                                () -> {
                                    LOG.errorContext(requestId, new Exception(String.format("User %s was not found", userEmail)), null);
                                    return new NotFoundRuntimeException(
                                            StaticPropertyUtil.APPLICATION_NAMESPACE,
                                            PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
                                });
        final var user = usersList.getFirst();

        acspProfileService.fetchAcspProfile(acspNumber);

        LOG.debugContext(getXRequestId(), String.format("Attempting to fetch memberships for Acsp %s and user %s", acspNumber, requestBody.getUserEmail()), null);
        final var acspMembershipsList =
                acspMembersService.fetchAcspMemberships(user, includeRemoved, acspNumber);

        LOG.infoContext(requestId, String.format("Successfully fetched memberships for Acsp %s and user %s", acspNumber, requestBody.getUserEmail()), null);

        return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
    }

    @Override
    public ResponseEntity<AcspMembershipsList> getMembersForAcsp(
            final String acspNumber,
            final String requestId,
            final Boolean includeRemoved,
            final Integer pageIndex,
            final Integer itemsPerPage,
            final String role) {

        LOG.infoContext(requestId, String.format("Received request with acsp_number=%s, include_removed=%b, page_index=%d, items_per_page=%d, role=%s", acspNumber, includeRemoved, pageIndex, itemsPerPage, role), null);

        final boolean roleIsValid =
                Optional.ofNullable(role)
                        .map(
                                theRole ->
                                        Arrays.stream(AcspMembership.UserRoleEnum.values())
                                                .map(AcspMembership.UserRoleEnum::getValue)
                                                .anyMatch(validRole -> validRole.equals(theRole)))
                        .orElse(true);

        if (!roleIsValid) {
            LOG.errorContext(requestId, new Exception(String.format("Role was invalid: %s", role)), null);
            throw new BadRequestRuntimeException(PLEASE_CHECK_THE_REQUEST_AND_TRY_AGAIN);
        }

        final var paginationParams =
                PaginationValidatorUtil.validateAndGetParams(pageIndex, itemsPerPage);

        final var acspProfile = acspProfileService.fetchAcspProfile(acspNumber);

        LOG.debugContext(requestId, "Attempting to fetch memberships", null);
        final var acspMembershipsList =
                acspMembersService.findAllByAcspNumberAndRole(
                        acspNumber,
                        acspProfile,
                        role,
                        includeRemoved,
                        paginationParams.pageIndex,
                        paginationParams.itemsPerPage);

        LOG.infoContext(requestId, String.format("Successfully to retrieved members for Acsp %s", acspNumber), null);

        return new ResponseEntity<>(acspMembershipsList, HttpStatus.OK);
    }
}
