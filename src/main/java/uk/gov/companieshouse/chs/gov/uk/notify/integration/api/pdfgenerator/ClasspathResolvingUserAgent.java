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

    @Override
    protected InputStream resolveAndOpenStream(String uri) {
        if (!isInJar(uri)) {
            return super.resolveAndOpenStream(uri);
        } else {
            // If the resource is a file in a jar file, then strip off the jar file path from the
            // resource URI before presenting the resource's jar-root-relative path URI to
            // getResourceAsStream.
            uri = stripOffJarContext(uri);
            return this.getClass().getResourceAsStream(uri);
        }
    }

    /**
     * Uses the presence of a <code>!</code> symbol in the URI to infer it is a URI referring to a
     * file embedded within a jar file.
     * @param uri the URI of a resource which may be held within a jar
     * @return whether the resource referred to is in jar file (<code>true</code>), or not
     *      (<code>false</code>).
     */
    private boolean isInJar(String uri) {
        return uri.contains("!");
    }

    /**
     * Takes a URI such as
     * <code>file:/app/classpath/chs-gov-uk-notify-integration-api-unversioned.original.jar!
     * /assets/templates/letters/common/notify_letter_styles.css"</code>
     * and returns the same path without its Jar context,
     * <code>/assets/templates/letters/common/notify_letter_styles.css</code>.
     *
     * @param uri the URI which may include a JAR context
     * @return the same URI minus any jar context or prefix, leaving only the relative path of the
     *      resource sought within the jar.
     */
    private String stripOffJarContext(String uri) {
        return uri.substring(uri.lastIndexOf('!') + 1);
    }
}
