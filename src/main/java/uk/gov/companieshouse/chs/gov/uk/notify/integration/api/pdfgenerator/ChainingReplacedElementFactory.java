package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.pdfgenerator;

import java.util.ArrayList;
import java.util.List;
import org.w3c.dom.Element;
import org.xhtmlrenderer.extend.ReplacedElement;
import org.xhtmlrenderer.extend.ReplacedElementFactory;
import org.xhtmlrenderer.extend.UserAgentCallback;
import org.xhtmlrenderer.layout.LayoutContext;
import org.xhtmlrenderer.render.BlockBox;
import org.xhtmlrenderer.simple.extend.FormSubmissionListener;

public class ChainingReplacedElementFactory implements ReplacedElementFactory {
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

    @Override
    public void reset() {
        for (var replacedElementFactory : replacedElementFactories) {
            replacedElementFactory.reset();
        }
    }

    @Override
    public void remove(Element element) {
        for (var replacedElementFactory : replacedElementFactories) {
            replacedElementFactory.remove(element);
        }
    }

    @Override
    public void setFormSubmissionListener(FormSubmissionListener listener) {
        for (var replacedElementFactory : replacedElementFactories) {
            replacedElementFactory.setFormSubmissionListener(listener);
        }
    }
}
