package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatepersonalisation;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import org.springframework.stereotype.Component;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.templatelookup.ChLetterTemplate;

@Component
public class TemplatePersonaliser {

    /**
     * Populates the letter Thymeleaf template with the data for the letter.
     * @param template the {@link ChLetterTemplate} identifying the template to be used
     * @param  personalisationDetails the {@link Map} providing the data to be substituted into the
     *               letter template substitution variables
     * @return the HTML representation of the letter
     */
    public String personaliseLetterTemplate(ChLetterTemplate template,
                                            Map<String, String> personalisationDetails) {

        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        var filepath = "assets/templates/letters/chips/direction/v1/";
        templateResolver.setPrefix(filepath);

        var templateEngine = new TemplateEngine();
        templateEngine.addTemplateResolver(templateResolver);

        var context = new Context();

        // Use today's date for traceability.
        var format = DateTimeFormatter.ofPattern("dd MMMM yyyy");
        context.setVariable("date", LocalDate.now().format(format));

        personalisationDetails.keySet().forEach(name ->
                context.setVariable(name, personalisationDetails.get(name)));

        return templateEngine.process(template.id(), context);
    }
}
