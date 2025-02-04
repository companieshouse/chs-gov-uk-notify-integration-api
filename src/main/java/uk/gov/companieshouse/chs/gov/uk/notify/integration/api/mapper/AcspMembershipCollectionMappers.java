package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.mapper;

import org.mapstruct.Mapper;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;
import uk.gov.companieshouse.api.accounts.user.model.User;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembership;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsList;
import uk.gov.companieshouse.api.acsp_manage_users.model.AcspMembershipsListLinks;
import uk.gov.companieshouse.api.acspprofile.AcspProfile;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Component
@Mapper(componentModel = "spring")
public abstract class AcspMembershipCollectionMappers extends AcspMembershipMapper {

    private static final String END_POINT_URL_TEMPLATE = "/acsps/%s/memberships";

    private static final String PAGINATION_URL_TEMPLATE = "%s?page_index=%d&items_per_page=%d";

    private AcspMembershipsList enrichWithMetadata(final Page<AcspMembership> page, final String endpointUrl) {
        final var pageIndex = page.getNumber();
        final var itemsPerPage = page.getSize();
        final var self = String.format(PAGINATION_URL_TEMPLATE, endpointUrl, pageIndex, itemsPerPage);
        final var next = page.isLast() ? "" : String.format(PAGINATION_URL_TEMPLATE, endpointUrl, pageIndex + 1, itemsPerPage);
        final var previous = page.isFirst() ? "" : String.format(PAGINATION_URL_TEMPLATE, endpointUrl, pageIndex - 1, itemsPerPage);
        final var links = new AcspMembershipsListLinks().self(self).next(next).previous(previous);

        return new AcspMembershipsList()
                .items(page.getContent())
                .pageNumber(pageIndex)
                .itemsPerPage(itemsPerPage)
                .totalResults((int) page.getTotalElements())
                .totalPages(page.getTotalPages())
                .links(links);
    }

    public List<AcspMembership> daoToDto(final List<AcspMembersDao> acspMembers, final User userData, final AcspProfile acspProfile) {
        final var users = Objects.isNull(userData) ? usersService.fetchUserDetails(acspMembers.stream()) : Map.of(userData.getUserId(), userData);
        final var acsps = Objects.isNull(acspProfile) ? acspProfileService.fetchAcspProfiles(acspMembers.stream()) : Map.of(acspProfile.getNumber(), acspProfile);
        return acspMembers.stream()
                .map(dao -> daoToDto(dao, users.get(dao.getUserId()), acsps.get(dao.getAcspNumber())))
                .collect(Collectors.toList());
    }

    public AcspMembershipsList daoToDto(final Page<AcspMembersDao> acspMembers, final User userData, final AcspProfile acspProfile) {
        if (Objects.isNull(acspProfile)) {
            throw new IllegalArgumentException("acspProfile cannot be null.");
        }
        final var users = Objects.isNull(userData) ? usersService.fetchUserDetails(acspMembers.stream()) : Map.of(userData.getUserId(), userData);
        final var acspMemberships = acspMembers.map(dao -> daoToDto(dao, users.get(dao.getUserId()), acspProfile));
        return enrichWithMetadata(acspMemberships, String.format(END_POINT_URL_TEMPLATE, acspProfile.getNumber()));
    }

}