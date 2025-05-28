package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import java.io.IOException;
import java.net.URL;
import org.apache.batik.anim.dom.SAXSVGDocumentFactory;
import org.apache.batik.dom.svg.SVGDocumentFactory;
import org.apache.batik.util.XMLResourceDescriptor;
import org.springframework.stereotype.Component;
import org.w3c.dom.Element;
import org.w3c.dom.svg.SVGDocument;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.exception.SvgImageException;
import uk.gov.companieshouse.logging.Logger;

@Component
public class SvgReplacedElementFactory implements ReplacedElementFactory {

    private final Logger logger;

    public SvgReplacedElementFactory(Logger logger) {
        this.logger = logger;
    }

    @Override
    public ReplacedElement createReplacedElement(LayoutContext layoutContext,
                                                 BlockBox box,
                                                 UserAgentCallback uac,
                                                 int cssWidth,
                                                 int cssHeight) {
        var element = box.getElement();
        var imageFilepath = element.getAttribute("src");
        if ("img".equals(element.getNodeName()) && imageFilepath.endsWith(".svg")) {
            var factory = getDocumentFactory();

            SVGDocument svgImage;
            var url = getResourceUrl(imageFilepath);
            if (url == null) {
                var error = "SVG image not found: " + imageFilepath;
                logger.error(error);
                throw new SvgImageException("SVG image not found: " + imageFilepath);
            }
            try {
                svgImage = factory.createSVGDocument(url.toString());
            } catch (IOException ioException) {
                var error = "Caught IOException while creating SVG image " + imageFilepath
                        + ": " + ioException.getMessage();
                logger.error(error, ioException);
                throw new SvgImageException(error);
            }
            var svgElement = svgImage.getDocumentElement();
            var htmlDoc = element.getOwnerDocument();
            var importedNode = htmlDoc.importNode(svgElement, true);
            element.appendChild(importedNode);
            return new SvgReplacedElement(svgImage, cssWidth, cssHeight);
        }
        return null;
    }

    public URL getResourceUrl(String resourceFilepath) {
        return getClass().getClassLoader().getResource(resourceFilepath);
    }

    public SVGDocumentFactory getDocumentFactory() {
        return new SAXSVGDocumentFactory(XMLResourceDescriptor.getXMLParserClassName());
    }

    @Override
    public void reset() {
        // No PDF rendering related state to reset here.
    }

    @Override
    public void remove(Element element) {
        // No PDF rendering related state to remove any element from here.
    }

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {
        // No PDF rendering related state to add any form submission listener to here.
    }
}
