package com.chaekchaek.auth.oauth.google;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.client.oidc.userinfo.OidcUserService;

@Configuration
public class GoogleOidcConfig {

    @Bean
    OidcUserService oidcUserService() {
        return new OidcUserService();
    }
}
