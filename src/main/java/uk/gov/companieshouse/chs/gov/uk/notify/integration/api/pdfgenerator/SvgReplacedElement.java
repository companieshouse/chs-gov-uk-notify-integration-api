package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import static org.xhtmlrenderer.css.style.CalculatedStyle.BOTTOM;
import static org.xhtmlrenderer.css.style.CalculatedStyle.LEFT;

import java.awt.Point;
import java.awt.print.PageFormat;
import java.awt.print.Paper;
import org.apache.batik.transcoder.TranscoderInput;
import org.apache.batik.transcoder.print.PrintTranscoder;
import org.w3c.dom.Document;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.pdf.ITextOutputDevice;
import org.xhtmlrenderer.pdf.ITextReplacedElement;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.render.PageBox;
import org.xhtmlrenderer.render.RenderingContext;

public class SvgReplacedElement implements ITextReplacedElement {

    private final Point location = new Point(0, 0);
    private final Document svg;
    private final int cssWidth;
    private final int cssHeight;

    /**
     * Constructor.
     * @param svg the SVG image
     * @param cssWidth the width of the image
     * @param cssHeight the height of the image
     */
    public SvgReplacedElement(Document svg, int cssWidth, int cssHeight) {
        this.cssWidth = cssWidth;
        this.cssHeight = cssHeight;
        this.svg = svg;
    }

    @Override
    public void detach(LayoutContext layoutContext) {
        // No dynamic behaviour anticipated for this SVG element. Once it's in the document,
        // it should stay there. Hence, nothing to implement here.
    }

    @Override
    public int getBaseline() {
        return 0;
    }

    @Override
    public int getIntrinsicWidth() {
        return cssWidth;
    }

    @Override
    public int getIntrinsicHeight() {
        return cssHeight;
    }

    @Override
    public boolean hasBaseline() {
        return false;
    }

    @Override
    public boolean isRequiresInteractivePaint() {
        return false;
    }

    @Override
    public Point getLocation() {
        return location;
    }

    // x and y are from a universal naming convention
    @SuppressWarnings("checkstyle:ParameterName")
    @Override
    public void setLocation(int x, int y) {
        this.location.x = x;
        this.location.y = y;
    }

    // x and y are from a universal naming convention
    @SuppressWarnings("checkstyle:LocalVariableName")
    @Override
    public void paint(RenderingContext renderingContext, ITextOutputDevice outputDevice,
                      BlockBox blockBox) {

        float width = cssWidth / outputDevice.getDotsPerPoint();
        float height = cssHeight / outputDevice.getDotsPerPoint();

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
        x = (int)(x / outputDevice.getDotsPerPoint());
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
        var bottom = page.getPageNo() == 0 ? page.getBottom() : p0bottom + length;
        var y = (bottom - (blockBox.getAbsY() + cssHeight)) + page.getMarginBorderPadding(
                renderingContext, BOTTOM);
        y = (int)(y / outputDevice.getDotsPerPoint());
        return y;
    }
}
