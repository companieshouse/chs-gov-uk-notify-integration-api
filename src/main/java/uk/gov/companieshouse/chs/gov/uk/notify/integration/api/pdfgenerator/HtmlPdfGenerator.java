package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import com.lowagie.text.pdf.BaseFont;
import com.lowagie.text.pdf.PdfWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import org.springframework.stereotype.Service;
import org.xhtmlrenderer.pdf.ITextRenderer;
import org.xhtmlrenderer.pdf.util.XHtmlMetaToPdfInfoAdapter;

@Service
public class HtmlPdfGenerator {

    private static final String COMMON_ASSETS_DIRECTORY = "assets/templates/letters/common/";

    private final SvgReplacedElementFactory svgReplacedElementFactory;

    public HtmlPdfGenerator(SvgReplacedElementFactory svgReplacedElementFactory) {
        this.svgReplacedElementFactory = svgReplacedElementFactory;
    }

    /**
     * Generates a PDF from the HTML provided, saving it to the user home directory under the name
     * "directionLetter_&lt;reference&gt;.pdf".
     * @param html the final HTML representation of the document to be generated as a PDF
     * @param reference the reference used to identify the document and name the file containing
     *                  its PDF rendering
     * @return the {@link InputStream} for the PDF as written out to the file
     * @throws IOException should something go wrong whilst creating or saving the PDF
     */
    public InputStream generatePdfFromHtml(String html,
                                           String reference) throws IOException {
        var pdfFilepath = System.getProperty("user.home") + File.separator
                + "directionLetter_" + reference + ".pdf";
        try (var outputStream = new FileOutputStream(pdfFilepath)) {
            generatePdfFromHtml(html, outputStream);
        }
        return new FileInputStream(pdfFilepath);
    }

    public void generatePdfFromHtml(String html, OutputStream outputStream) throws IOException {

        var renderer = new ITextRenderer();

        // Configure "Accessible" PDF/A conformance level PDF/A-1a.
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

    private void addFont(final ITextRenderer renderer, final String fontFilename)
        throws IOException {
        renderer.getFontResolver().addFont(COMMON_ASSETS_DIRECTORY + fontFilename,
                BaseFont.IDENTITY_H, BaseFont.EMBEDDED);
    }

}
