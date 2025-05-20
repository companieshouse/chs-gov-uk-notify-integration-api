package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.thymeleaf.ITemplateEngine;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.templatemode.TemplateMode;
import org.thymeleaf.templateresolver.AbstractConfigurableTemplateResolver;
import org.thymeleaf.templateresolver.ClassLoaderTemplateResolver;
import org.thymeleaf.templateresolver.ITemplateResolver;

@Configuration
public class ThymeleafConfig {

    /**
     * Creates ITemplateResolver which helps the application locate letter templates and their
     * constituent files.
     *
     * @return the letter template resolver
     */
    @Bean
    public AbstractConfigurableTemplateResolver getTemplateResolver() {
        var templateResolver = new ClassLoaderTemplateResolver();
        templateResolver.setSuffix(".html");
        templateResolver.setTemplateMode(TemplateMode.HTML);

        // DISABLE CACHING - this will impair performance, but it will avoid
        // the (unlikely) possibility that a cached template is returned rather than
        // an error being raised as should occur when the same template is sought in the wrong
        // asset directory location.
        // This can otherwise occur because the cache key uses the template name but
        // not the prefix (= asset directory location).
        templateResolver.setCacheable(false);

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
