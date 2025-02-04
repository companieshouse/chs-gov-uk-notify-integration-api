package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils;

import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exceptions.BadRequestRuntimeException;
import uk.gov.companieshouse.logging.Logger;
import uk.gov.companieshouse.logging.LoggerFactory;

import static uk.gov.companieshouse.chs.gov.uk.notify.integration.api.utils.RequestContextUtil.getXRequestId;

public class PaginationValidatorUtil {

    private static final Logger LOG =
            LoggerFactory.getLogger(StaticPropertyUtil.APPLICATION_NAMESPACE);
    private static final int DEFAULT_PAGE_INDEX = 0;
    private static final int DEFAULT_ITEMS_PER_PAGE = 15;

    private PaginationValidatorUtil() {
        // private instructor to hide the implicit public one
    }

    public static class PaginationParams {
        public final int pageIndex;
        public final int itemsPerPage;

        private PaginationParams(int pageIndex, int itemsPerPage) {
            this.pageIndex = pageIndex;
            this.itemsPerPage = itemsPerPage;
        }
    }

    public static PaginationParams validateAndGetParams(Integer pageIndex, Integer itemsPerPage) {
        int validatedPageIndex = (pageIndex == null) ? DEFAULT_PAGE_INDEX : pageIndex;
        int validatedItemsPerPage = (itemsPerPage == null) ? DEFAULT_ITEMS_PER_PAGE : itemsPerPage;

        if (validatedPageIndex < 0) {
            LOG.errorContext(getXRequestId(), new Exception("pageIndex was less than 0"), null);
            throw new BadRequestRuntimeException("Please check the request and try again");
        }

        if (validatedItemsPerPage <= 0) {
            LOG.errorContext(getXRequestId(), new Exception("itemsPerPage was less than or equal to 0"), null);
            throw new BadRequestRuntimeException("Please check the request and try again");
        }

        return new PaginationParams(validatedPageIndex, validatedItemsPerPage);
    }
}
