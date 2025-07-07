package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import static org.xhtmlrenderer.css.style.CalculatedStyle.BOTTOM;
import static org.xhtmlrenderer.css.style.CalculatedStyle.LEFT;

import java.awt.print.PageFormat;
import java.awt.print.Paper;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.w3c.dom.Document;
import org.xhtmlrenderer.pdf.EmptyReplacedElement;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.PageBox;
import org.xhtmlrenderer.render.RenderingContext;

public class SvgReplacedElement extends EmptyReplacedElement {

    private final Document svg;

    /**
     * Constructor.
     * @param svg the SVG image
     * @param cssWidth the width of the image
     * @param cssHeight the height of the image
     */
    public SvgReplacedElement(Document svg, int cssWidth, int cssHeight) {
        super(cssWidth, cssHeight);
        this.svg = svg;
    }

    // x and y are from a universal naming convention
    @SuppressWarnings("checkstyle:LocalVariableName")
    @Override
    public void paint(RenderingContext renderingContext, ITextOutputDevice outputDevice,
                      BlockBox blockBox) {

        float width = getIntrinsicWidth() / outputDevice.getDotsPerPoint();
        float height = getIntrinsicHeight() / outputDevice.getDotsPerPoint();

        var prm = new PrintTranscoder();
        var ti = new TranscoderInput(svg);
        prm.transcode(ti, null);
        var pg = new PageFormat();
        var pp = new Paper();
        pp.setSize(width, height);
        pp.setImageableArea(0, 0, width, height);
        pg.setPaper(pp);
        var cb = outputDevice.getWriter().getDirectContent();
        var template = cb.createTemplate(width, height);
        var g2d = template.createGraphics(width, height);
        prm.print(g2d, pg, 0);
        g2d.dispose();

        var x = calculateSvgImageXCoordinate(renderingContext, outputDevice, blockBox);
        var y = calculateSvgImageYCoordinate(renderingContext, outputDevice, blockBox);

        cb.addTemplate(template, x, y);
    }

    // x and y are from a universal naming convention
    @SuppressWarnings("checkstyle:LocalVariableName")
    private int calculateSvgImageXCoordinate(RenderingContext renderingContext,
                                             ITextOutputDevice outputDevice,
                                             BlockBox blockBox) {
        var page = renderingContext.getPage();
        var x = blockBox.getAbsX() + page.getMarginBorderPadding(renderingContext, LEFT);
        x = (int) (x / outputDevice.getDotsPerPoint());
        return x;
    }

    // x and y are from a universal naming convention
    @SuppressWarnings("checkstyle:LocalVariableName")
    private int calculateSvgImageYCoordinate(RenderingContext renderingContext,
                                             ITextOutputDevice outputDevice,
                                             BlockBox blockBox) {
        var page = renderingContext.getPage();
        var p0bottom =
                ((PageBox) renderingContext.getRootLayer().getPages().getFirst()).getBottom();
        var length = page.getBottom() - page.getTop();

        var oddPageBottom = isHeaderOrFooter() ? p0bottom : page.getBottom();
        var evenPageBottom = p0bottom + length;
        var bottom = (page.getPageNo() % 2 == 0) ? oddPageBottom : evenPageBottom;

        var y = (bottom - (blockBox.getAbsY() + getIntrinsicHeight()))
                + page.getMarginBorderPadding(renderingContext, BOTTOM);
        y = (int) (y / outputDevice.getDotsPerPoint());
        return y;
    }

    private boolean isHeaderOrFooter() {
        return svg.getDocumentURI().contains("logo") || svg.getDocumentURI().contains("footer");
    }
}
