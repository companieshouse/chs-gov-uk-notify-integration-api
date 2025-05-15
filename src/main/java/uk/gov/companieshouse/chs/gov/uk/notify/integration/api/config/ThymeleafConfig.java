package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class ThymeleafConfig {

    private static final String FIRST_TEMPLATE_FILEPATH = "assets/templates/letters/chips/";

    /**
     * Creates ITemplateResolver which helps the application locate letter templates and their
     * constituent files.
     *
     * @return the letter template resolver
     */
    @Bean
    public ITemplateResolver getTemplateResolver() {
        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);
        templateResolver.setPrefix(FIRST_TEMPLATE_FILEPATH);
        return templateResolver;
    }

    /**
     * Creates ITemplateEngine used by the application to parse Thymeleaf templates for producing
     * letter content.
     *
     * @param templateResolver the resolver the template engine uses to help locate letter
     *                         templates and their constituent files
     * @return the letter template engine
     */
    @Bean
    public ITemplateEngine getTemplateEngine(ITemplateResolver templateResolver) {
        var templateEngine = new TemplateEngine();
        templateEngine.addTemplateResolver(templateResolver);
        return templateEngine;
    }

}
