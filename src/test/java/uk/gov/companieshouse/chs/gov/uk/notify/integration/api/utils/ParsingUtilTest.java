package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.common.TestDataManager;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.InternalServerErrorRuntimeException;
import uk.gov.companieshouse.api.accounts.user.model.User;

@ExtendWith(MockitoExtension.class)
@Tag("unit-test")
class ParsingUtilTest {

    private static final TestDataManager testDataManager = TestDataManager.getInstance();

    @Test
    void parseJsonToSuccessfullyParsesToSpecifiedClass() throws JsonProcessingException {
        final var user = testDataManager.fetchUserDtos("WITU001").getFirst();
        final var json = new ObjectMapper().writeValueAsString(user);
        Assertions.assertEquals("Geralt of Rivia", ParsingUtil.parseJsonTo(User.class).apply(json).getDisplayName());
    }

    @Test
    void parseJsonToWithArbitraryErrorThrowsInternalServerErrorRuntimeException() {
        Assertions.assertThrows(InternalServerErrorRuntimeException.class, () -> ParsingUtil.parseJsonTo(User.class).apply("}{"));
    }


}
