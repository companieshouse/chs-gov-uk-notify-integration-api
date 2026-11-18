package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

public record AttachmentFile(byte[] content, String filename) {

    public AttachmentFile(String filename,
                          byte[] content) {
        this(content, filename);
    }
}
