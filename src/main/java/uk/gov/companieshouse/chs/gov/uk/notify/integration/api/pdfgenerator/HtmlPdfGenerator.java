package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import java.io.BufferedOutputStream;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.util.XHtmlMetaToPdfInfoAdapter;
import uk.gov.companieshouse.logging.Logger;

@Component
public class HtmlPdfGenerator {

    private static final String COMMON_ASSETS_DIRECTORY = "assets/templates/letters/common/";
    private final SvgReplacedElementFactory svgReplacedElementFactory;
    private final boolean saveLetter;
    private final Logger logger;

    public HtmlPdfGenerator(final SvgReplacedElementFactory svgReplacedElementFactory,
                            @Value("${save.letter:false}") final boolean saveLetter,
                            final Logger logger) {
        this.svgReplacedElementFactory = svgReplacedElementFactory;
        this.saveLetter = saveLetter;
        this.logger = logger;
    }

    /**
     * Generates a PDF from the HTML provided. If the <code>save.letter</code> property is
     * <code>true</code>, it saves the letter PDF to the user home directory under the name
     * "letter_&lt;reference&gt;.pdf", otherwise it holds the PDF in memory only.
     *
     * @param html the final HTML representation of the document to be generated as a PDF
     * @param reference the reference used to identify the document and name the file containing
     *                  its PDF rendering
     * @return the {@link InputStream} for the PDF as written out to the file
     * @throws IOException should something go wrong whilst creating or saving the PDF
     */
    public InputStream generatePdfFromHtml(String html,
                                           String reference) throws IOException {
        if (saveLetter) {
            return getPdfFileInputStream(html, reference);
        } else {
            return getPdfInMemoryInputStream(html);
        }
    }

    public void generatePdfFromHtml(String html, OutputStream outputStream) throws IOException {

        var renderer = new ITextRenderer();

        // Configure "Accessible" PDF/A conformance level PDF/A-1a.
        // Even if this may seem pointless given that Gov Notify manipulate the
        // PDF before printing and sending the letter, setting this also has the
        // positive side effect of alerting us indirectly to the fact when stylesheets
        // cannot be found.
        renderer.setPDFVersion(PdfWriter.VERSION_1_4);
        renderer.setPDFXConformance(PdfWriter.PDFA1A);
        renderer.setColourSpaceProfile(
                "/" + COMMON_ASSETS_DIRECTORY + "sRGB Color Space Profile.icm");

        // Register Arial fonts to be able to use them in the PDF.
        // Otherwise, we get Helvetica despite having styled Arial in the CSS!
        addFont(renderer, "Arial.ttf");
        addFont(renderer, "Arial Bold.ttf");

        // Try to handle SVG image as per https://stackoverflow.com/questions/37056791/svg-integration-in-pdf-using-flying-saucer.
        var chainingReplacedElementFactory = new ChainingReplacedElementFactory();
        chainingReplacedElementFactory.addReplacedElementFactory(
                renderer.getSharedContext().getReplacedElementFactory());
        chainingReplacedElementFactory.addReplacedElementFactory(svgReplacedElementFactory);
        renderer.getSharedContext().setReplacedElementFactory(chainingReplacedElementFactory);

        var resolvingUserAgent = new ClasspathResolvingUserAgent(renderer.getOutputDevice());
        resolvingUserAgent.setSharedContext(renderer.getSharedContext());
        renderer.getSharedContext().setUserAgentCallback(resolvingUserAgent);
        renderer.setDocumentFromString(html);
        renderer.layout();

        // This gets the "creator" metadata into the PDF info as "Author".
        var metaToPdfInfoAdapter = new XHtmlMetaToPdfInfoAdapter(renderer.getDocument());
        renderer.setListener(metaToPdfInfoAdapter);

        renderer.createPDF(outputStream);
    }

    /**
     * Generates a PDF from the HTML provided, holding it in memory.
     *
     * @param html the final HTML representation of the document to be generated as a PDF
     * @return the {@link InputStream} for the PDF as written out to the file
     * @throws IOException should something go wrong whilst creating or writing the PDF
     */
    private InputStream getPdfInMemoryInputStream(String html) throws IOException {
        InputStream in;
        try (var outputStream = new ByteArrayOutputStream()) {
            generatePdfFromHtml(html, outputStream);
            in = new ByteArrayInputStream(outputStream.toByteArray());
        }
        return in;
    }

    /**
     * Generates a PDF from the HTML provided, saving it to the user home directory under the name
     * "letter_&lt;reference&gt;.pdf".
     *
     * @param html the final HTML representation of the document to be generated as a PDF
     * @param reference the reference used to identify the document and name the file containing
     *                  its PDF rendering
     * @return the {@link InputStream} for the PDF as written out to the file
     * @throws IOException should something go wrong whilst creating or saving the PDF
     */
    @SuppressWarnings("java:S6300") // This is not a mobile application.
    private InputStream getPdfFileInputStream(String html,
                                              String reference) throws IOException {
        var pdfFilepath = getPdfFilepath(reference);
        logger.info("Saving PDF of letter to " + pdfFilepath + ".");
        try (var outputStream = new BufferedOutputStream(new FileOutputStream(pdfFilepath))) {
            generatePdfFromHtml(html, outputStream);
        }
        return new FileInputStream(pdfFilepath);
    }

    public static String getPdfFilepath(String reference) {
        return System.getProperty("user.home") + File.separator
                + "letter_" + reference + ".pdf";
    }

    private void addFont(final ITextRenderer renderer, final String fontFilename)
        throws IOException {
        renderer.getFontResolver().addFont(COMMON_ASSETS_DIRECTORY + fontFilename,
                BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    }

}
