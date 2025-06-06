package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import java.util.ArrayList;
import java.util.List;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.NoReplacedElementFactory;

public class ChainingReplacedElementFactory extends NoReplacedElementFactory {
    private final List<ReplacedElementFactory> replacedElementFactories
            = new ArrayList<>();

    public void addReplacedElementFactory(ReplacedElementFactory replacedElementFactory) {
        replacedElementFactories.addFirst(replacedElementFactory);
    }

    @Override
    public ReplacedElement createReplacedElement(LayoutContext layoutContext,
                                                 BlockBox box,
                                                 UserAgentCallback uac,
                                                 int cssWidth,
                                                 int cssHeight) {
        for (var replacedElementFactory : replacedElementFactories) {
            var element =
                    replacedElementFactory.createReplacedElement(layoutContext, box, uac, cssWidth,
                            cssHeight);
            if (element != null) {
                return element;
            }
        }
        return null;
    }

}
