package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClientResponseException;
import reactor.core.publisher.Mono;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.NotFoundRuntimeException;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.model.AcspMembersDao;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class UsersServiceTest {

    @Mock
    private WebClient usersWebClient;

    @InjectMocks
    private UsersService usersService;

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    private void mockWebClientSuccessResponse(final String uri, final Mono<String> jsonResponse) {
        final var requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        final var requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        Mockito.doReturn(requestHeadersUriSpec).when(usersWebClient).get();
        Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(uri);
        Mockito.doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        Mockito.doReturn(jsonResponse).when(responseSpec).bodyToMono(String.class);
    }

    private void mockWebClientForFetchUserDetails(final String userId) throws JsonProcessingException {
        final var user = testDataManager.fetchUserDtos(userId).getFirst();
        final var uri = String.format("/users/%s", userId);
        final var jsonResponse = new ObjectMapper().writeValueAsString(user);
        mockWebClientSuccessResponse(uri, Mono.just(jsonResponse));
    }

    private void mockWebClientErrorResponse(final String uri, int responseCode) {
        final var requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        final var requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        Mockito.doReturn(requestHeadersUriSpec).when(usersWebClient).get();
        Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(uri);
        Mockito.doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        Mockito.doReturn(Mono.error(new WebClientResponseException(responseCode, "Error", null, null, null))).when(responseSpec).bodyToMono(String.class);
    }

    private void mockWebClientForFetchUserDetailsErrorResponse(final String userId, int responseCode) {
        final var uri = String.format("/users/%s", userId);
        mockWebClientErrorResponse(uri, responseCode);
    }

    private void mockWebClientJsonParsingError(final String uri) {
        final var requestHeadersUriSpec = Mockito.mock(WebClient.RequestHeadersUriSpec.class);
        final var requestHeadersSpec = Mockito.mock(WebClient.RequestHeadersSpec.class);
        final var responseSpec = Mockito.mock(WebClient.ResponseSpec.class);

        Mockito.doReturn(requestHeadersUriSpec).when(usersWebClient).get();
        Mockito.doReturn(requestHeadersSpec).when(requestHeadersUriSpec).uri(uri);
        Mockito.doReturn(responseSpec).when(requestHeadersSpec).retrieve();
        Mockito.doReturn(Mono.just("}{")).when(responseSpec).bodyToMono(String.class);
    }

    private void mockWebClientForFetchUserDetailsJsonParsingError(final String userId) {
        final var uri = String.format("/users/%s", userId);
        mockWebClientJsonParsingError(uri);
    }

    private void mockWebClientForSearchUserDetails(final String... userIds) throws JsonProcessingException {
        final var users = testDataManager.fetchUserDtos(userIds);
        final var uri = String.format("/users/search?user_email=" + String.join("&user_email=", users.stream().map(User::getEmail).toList()));
        final var jsonResponse = new ObjectMapper().writeValueAsString(users);
        mockWebClientSuccessResponse(uri, Mono.just(jsonResponse));
    }

    private void mockWebClientForSearchUserDetailsErrorResponse(final String userEmail, int responseCode) {
        final var uri = String.format("/users/search?user_email=%s", userEmail);
        mockWebClientErrorResponse(uri, responseCode);
    }

    private void mockWebClientForSearchUserDetailsNonexistentEmail(final String... emails) {
        final var uri = String.format("/users/search?user_email=" + String.join("&user_email=", Arrays.stream(emails).toList()));
        mockWebClientSuccessResponse(uri, Mono.empty());
    }

    private void mockWebClientForSearchUserDetailsJsonParsingError(final String... emails) {
        final var uri = String.format("/users/search?user_email=" + String.join("&user_email=", Arrays.stream(emails).toList()));
        mockWebClientJsonParsingError(uri);
    }

    @Test
    void fetchUserDetailsForNullOrNonexistentUserReturnsNotFoundRuntimeException() {
        mockWebClientForFetchUserDetailsErrorResponse(null, 404);
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> usersService.fetchUserDetails((String) null));

        mockWebClientForFetchUserDetailsErrorResponse("404User", 404);
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> usersService.fetchUserDetails("404User"));
    }

    @Test
    void fetchUserDetailsWithMalformedUserIdReturnsInternalServerErrorRuntimeException() {
        mockWebClientForFetchUserDetailsErrorResponse("£$@123", 400);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails("£$@123"));
    }

    @Test
    void fetchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockWebClientForFetchUserDetailsJsonParsingError("WITU001");
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails("WITU001"));
    }

    @Test
    void fetchUserDetailsReturnsSpecifiedUser() throws JsonProcessingException {
        mockWebClientForFetchUserDetails("WITU001");
        Assertions.assertEquals("Geralt of Rivia", usersService.fetchUserDetails("WITU001").getDisplayName());
    }

    @Test
    void fetchUserDetailsWithNullStreamThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> usersService.fetchUserDetails((Stream<AcspMembersDao>) null));
    }

    @Test
    void fetchUserDetailsWithEmptyStreamReturnsEmptyMap() {
        Assertions.assertEquals(0, usersService.fetchUserDetails(Stream.of()).size());
    }

    @Test
    void fetchUserDetailsWithStreamThatHasNonExistentUserReturnsNotFoundRuntimeException() {
        final var membership = new AcspMembersDao();
        membership.setUserId("404User");
        mockWebClientForFetchUserDetailsErrorResponse("404User", 404);
        Assertions.assertThrows(NotFoundRuntimeException.class, () -> usersService.fetchUserDetails(Stream.of(membership)));
    }

    @Test
    void fetchUserDetailsWithStreamThatHasMalformedUserIdReturnsInternalServerErrorRuntimeException() {
        final var membership = new AcspMembersDao();
        membership.setUserId("£$@123");
        mockWebClientForFetchUserDetailsErrorResponse("£$@123", 400);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails(Stream.of(membership)));
    }

    @Test
    void fetchUserDetailsWithStreamWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        final var membership = testDataManager.fetchAcspMembersDaos("WIT001").getFirst();
        mockWebClientForFetchUserDetailsJsonParsingError("WITU001");
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.fetchUserDetails(Stream.of(membership)));
    }

    @Test
    void fetchUserDetailsWithStreamReturnsMap() throws JsonProcessingException {
        final var membership = testDataManager.fetchAcspMembersDaos("WIT001").getFirst();
        mockWebClientForFetchUserDetails("WITU001");
        final var users = usersService.fetchUserDetails(Stream.of(membership, membership));

        Assertions.assertEquals(1, users.size());
        Assertions.assertTrue(users.containsKey("WITU001"));
        Assertions.assertTrue(users.values().stream().map(User::getUserId).toList().contains("WITU001"));
    }

    @Test
    void searchUserDetailsWithNullListThrowsNullPointerException() {
        Assertions.assertThrows(NullPointerException.class, () -> usersService.searchUserDetails(null));
    }

    @Test
    void searchUserDetailWithNullOrMalformedUserEmailThrowsInternalServerErrorRuntimeException() {
        final var emails = new ArrayList<String>();
        emails.add(null);
        mockWebClientForSearchUserDetailsErrorResponse(null, 400);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails(emails));

        mockWebClientForSearchUserDetailsErrorResponse("£$@123", 400);
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails(List.of("£$@123")));
    }

    @Test
    void searchUserDetailsReturnsUsersList() throws JsonProcessingException {
        mockWebClientForSearchUserDetails("WITU001");
        final var result = usersService.searchUserDetails(List.of("geralt@witcher.com"));
        Assertions.assertEquals(1, result.size());
        Assertions.assertEquals("Geralt of Rivia", result.getFirst().getDisplayName());
    }

    @Test
    void searchUserDetailsWithNonexistentEmailReturnsNull() {
        mockWebClientForSearchUserDetailsNonexistentEmail("404@email.com");
        Assertions.assertNull(usersService.searchUserDetails(List.of("404@email.com")));
    }

    @Test
    void searchUserDetailsWithArbitraryErrorReturnsInternalServerErrorRuntimeException() {
        mockWebClientForSearchUserDetailsJsonParsingError("geralt@witcher.com");
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> usersService.searchUserDetails(List.of("geralt@witcher.com")));
    }

}
