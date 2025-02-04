package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import org.junit.jupiter.api.Test;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.BadRequestRuntimeException;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PaginationValidatorUtilTest {

    @Test
    void testValidateAndGetParamsWithValidParams() {
        final var params = PaginationValidatorUtil.validateAndGetParams(1, 10);
        assertEquals(1, params.pageIndex);
        assertEquals(10, params.itemsPerPage);
    }

    @Test
    void testValidateAndGetParamsWithNullParams() {
        final var params = PaginationValidatorUtil.validateAndGetParams(null, null);
        assertEquals(0, params.pageIndex);
        assertEquals(15, params.itemsPerPage);
    }

    @Test
    void testValidateAndGetParamsWithNegativePageIndex() {
        final var exception = assertThrows(BadRequestRuntimeException.class, () -> PaginationValidatorUtil.validateAndGetParams(-1, 10));
        assertEquals("Please check the request and try again", exception.getMessage());
    }

    @Test
    void testValidateAndGetParamsWithZeroItemsPerPage() {
        final var exception = assertThrows(BadRequestRuntimeException.class, () -> PaginationValidatorUtil.validateAndGetParams(1, 0));
        assertEquals("Please check the request and try again", exception.getMessage());
    }

    @Test
    void testValidateAndGetParamsWithNegativeItemsPerPage() {
        final var exception = assertThrows(BadRequestRuntimeException.class, () -> PaginationValidatorUtil.validateAndGetParams(1, -1));
        assertEquals("Please check the request and try again", exception.getMessage());
    }
}
