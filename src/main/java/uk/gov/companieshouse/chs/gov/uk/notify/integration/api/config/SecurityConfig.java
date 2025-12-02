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
import uk.gov.companieshouse.api.interceptor.InternalUserInterceptor;

@Configuration
@EnableWebSecurity
public class SecurityConfig implements WebMvcConfigurer {

    private final InternalUserInterceptor internalUserInterceptor;

    public SecurityConfig(InternalUserInterceptor internalUserInterceptor) {
        this.internalUserInterceptor = internalUserInterceptor;
    }

    @Bean
    @SuppressWarnings("java:S4502") // This is an internal API not at risk of CSRF attacks.
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
        registry.addInterceptor(internalUserInterceptor).addPathPatterns(
                "/gov-uk-notify-integration/letter",
                "/gov-uk-notify-integration/letters/view",
                "/gov-uk-notify-integration/letters/view/*",
                "/gov-uk-notify-integration/letters/paginated_view/*",
                "/gov-uk-notify-integration/letters/paginated_view/*/*",
                "/gov-uk-notify-integration/letters/view_by_reference/paginated_view/*");
    }

}