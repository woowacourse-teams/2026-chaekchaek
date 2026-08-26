package com.chaekchaek.auth.config;

import com.chaekchaek.auth.oauth.google.GoogleOidcUserService;
import com.chaekchaek.auth.handler.OAuth2AuthenticationFailureHandler;
import com.chaekchaek.auth.handler.OAuth2AuthenticationSuccessHandler;
import com.chaekchaek.auth.handler.RestAuthenticationEntryPoint;
import com.chaekchaek.auth.token.access.HeaderOrCookieBearerTokenResolver;
import java.util.Arrays;
import java.util.List;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

    private final OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler;
    private final OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler;
    private final GoogleOidcUserService googleOidcUserService;
    private final HeaderOrCookieBearerTokenResolver bearerTokenResolver;
    private final RestAuthenticationEntryPoint restAuthenticationEntryPoint;
    private final List<String> frontendAllowedOrigins;

    public SecurityConfig(
            OAuth2AuthenticationSuccessHandler oAuth2AuthenticationSuccessHandler,
            OAuth2AuthenticationFailureHandler oAuth2AuthenticationFailureHandler,
            GoogleOidcUserService googleOidcUserService,
            HeaderOrCookieBearerTokenResolver bearerTokenResolver,
            RestAuthenticationEntryPoint restAuthenticationEntryPoint,
            @Value("${app.frontend.allowed-origins}") String frontendAllowedOrigins
    ) {
        this.oAuth2AuthenticationSuccessHandler = oAuth2AuthenticationSuccessHandler;
        this.oAuth2AuthenticationFailureHandler = oAuth2AuthenticationFailureHandler;
        this.googleOidcUserService = googleOidcUserService;
        this.bearerTokenResolver = bearerTokenResolver;
        this.restAuthenticationEntryPoint = restAuthenticationEntryPoint;
        this.frontendAllowedOrigins = Arrays.stream(frontendAllowedOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .toList();
    }

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http)
            throws Exception {

        return http
                .cors(Customizer.withDefaults())
                .csrf(csrf -> csrf
                        .ignoringRequestMatchers(
                                "/api/v1/auth/**",
                                "/api/v1/books/*/reviews",
                                "/api/v1/reviews/*",
                                "/api/v1/reviews/*/replies",
                                "/api/v1/reviews/*/reactions",
                                "/api/v1/replies/*",
                                "/api/v1/replies/*/reactions"
                        ))
                .authorizeHttpRequests(auth -> auth
                        .requestMatchers(
                                HttpMethod.GET, "/health"
                        ).permitAll()
                        .requestMatchers(
                                "/oauth2/**",
                                "/login/oauth2/**",
                                "/api/v1/auth/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                "/docs/**",
                                "/webjars/swagger-ui/**"
                        ).permitAll()
                        .requestMatchers(
                                HttpMethod.GET,
                                PublicEndpointPaths.GET_ENDPOINTS
                        ).permitAll()
                        .requestMatchers(HttpMethod.POST,
                                "/api/v1/books/*/reviews",
                                "/api/v1/reviews/*/replies",
                                "/api/v1/reviews/*/reactions",
                                "/api/v1/replies/*/reactions"
                        ).permitAll()
                        .requestMatchers(HttpMethod.PATCH,
                                "/api/v1/reviews/*",
                                "/api/v1/replies/*"
                        ).permitAll()
                        .requestMatchers(HttpMethod.DELETE,
                                "/api/v1/reviews/*",
                                "/api/v1/replies/*",
                                "/api/v1/reviews/*/reactions",
                                "/api/v1/replies/*/reactions"
                        ).permitAll()
                        .anyRequest().authenticated()
                )
                .oauth2Login(oauth2 -> oauth2
                        .userInfoEndpoint(userInfo -> userInfo
                                .oidcUserService(googleOidcUserService)
                        )
                        .successHandler(oAuth2AuthenticationSuccessHandler)
                        .failureHandler(oAuth2AuthenticationFailureHandler)
                )
                .oauth2ResourceServer(resourceServer -> resourceServer
                        .bearerTokenResolver(bearerTokenResolver)
                        .authenticationEntryPoint(restAuthenticationEntryPoint)
                        .jwt(Customizer.withDefaults()))
                .build();
    }

    @Bean
    CorsConfigurationSource corsConfigurationSource() {
        CorsConfiguration configuration = new CorsConfiguration();

        configuration.setAllowedOrigins(frontendAllowedOrigins);
        configuration.setAllowedMethods(
                List.of("GET", "POST", "PUT", "PATCH", "DELETE", "OPTIONS")
        );
        configuration.setAllowedHeaders(List.of("*"));
        configuration.setAllowCredentials(true);

        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();

        source.registerCorsConfiguration("/**", configuration);

        return source;
    }
}
