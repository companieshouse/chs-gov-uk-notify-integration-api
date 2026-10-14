package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.service;

public class AttachmentFile {

    private final byte[] content;
    private final String filename;

    public AttachmentFile(String filename,
                          byte[] content) {
        this.filename = filename;
        this.content = content;
    }

    public byte[] getContent() {
        return content;
    }

    public String getFilename() {
        return filename;
    }
}
