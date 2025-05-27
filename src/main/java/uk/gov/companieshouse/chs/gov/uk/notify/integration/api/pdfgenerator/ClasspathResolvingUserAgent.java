package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import java.util.Optional;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextUserAgent;

public class ClasspathResolvingUserAgent extends ITextUserAgent {
    public ClasspathResolvingUserAgent(ITextOutputDevice outputDevice) {
        super(outputDevice);
    }

    @Override
    public String resolveURI(String fileNameOrUri) {
        // If resolvable as a classpath resource then do so, if not then fallback to flying saucer's
        // default URI resolution.
        var url = Optional.ofNullable(getClass().getClassLoader().getResource(fileNameOrUri));
        return url.isPresent() ? url.get().getPath() : super.resolveURI(fileNameOrUri);
    }
}
