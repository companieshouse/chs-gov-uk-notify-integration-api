package uk.gov.companieshouse.chs.gov.uk.notify.integration.api.config;

import static org.springframework.http.HttpMethod.GET;
import static org.springframework.http.HttpMethod.POST;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import uk.gov.companieshouse.chs.gov.uk.notify.integration.api.interceptor.ApiAuthorisationInterceptor;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final ApiAuthorisationInterceptor authorisationInterceptor;

    public SecurityConfig(ApiAuthorisationInterceptor authorisationInterceptor) {
        this.authorisationInterceptor = authorisationInterceptor;
    }

    @Bean
    public SecurityFilterChain filterChain(final HttpSecurity http) throws Exception {
        http.cors(AbstractHttpConfigurer::disable)
                .sessionManagement(s -> s.sessionCreationPolicy(SessionCreationPolicy.STATELESS))
                .csrf(AbstractHttpConfigurer::disable)
                .authorizeHttpRequests(request -> request
                        .requestMatchers(GET, "/gov-uk-notify-integration/**")
                        .permitAll()
                        .requestMatchers(POST, "/gov-uk-notify-integration/**")
                        .permitAll()
                        .anyRequest()
                        .denyAll())
        ;
        return http.build();
    }

    @Override
    public void addInterceptors(final InterceptorRegistry registry) {
        registry.addInterceptor(authorisationInterceptor).addPathPatterns(
                "/gov-uk-notify-integration/letter");
    }

}