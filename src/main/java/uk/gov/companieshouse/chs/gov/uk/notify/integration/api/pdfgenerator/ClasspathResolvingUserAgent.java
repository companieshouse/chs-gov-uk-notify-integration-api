package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import java.io.InputStream;
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

    // TODO DEEP-369 Sort this out.
    @Override
    protected InputStream resolveAndOpenStream(String uri) {

        InputStream is = null;
        uri = this.resolveURI(uri);

        uri = stripOffJarContext(uri);

        try {
            is = this.getClass().getResourceAsStream(uri);
        }  finally {
            System.out.println("Resolving " + uri + " to " + is);
        }

        return is;
    }

    /**
     * Takes a URI such as
     * <code>file:/app/classpath/chs-gov-uk-notify-integration-api-unversioned.original.jar!
     * /assets/templates/letters/common/notify_letter_layout.css"</code>
     * and returns the same path without its Jar context,
     * <code>/assets/templates/letters/common/notify_letter_layout.css</code>.
     *
     * @param uri the URI which may include a JAR context
     * @return the same URI minus any jar context or prefix, leaving only the relative path of the
     * resource sought within the jar.
     */
    private String stripOffJarContext(String uri) {
        return uri.substring(uri.lastIndexOf('!') + 1);
    }
}
