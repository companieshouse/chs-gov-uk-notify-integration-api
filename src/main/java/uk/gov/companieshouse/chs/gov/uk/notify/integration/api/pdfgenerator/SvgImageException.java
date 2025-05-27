package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

/**
 * Instances of this report issues rendering SVG images within the PDF.
 */
public class SvgImageException extends RuntimeException {
    public SvgImageException(String message) {
        super(message);
    }

    public SvgImageException(Throwable cause) {
        super(cause);
    }
}
