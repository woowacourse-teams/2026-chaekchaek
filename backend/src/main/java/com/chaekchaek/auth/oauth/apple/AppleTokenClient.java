package com.chaekchaek.auth.oauth.apple;

import com.chaekchaek.auth.exception.AppleAuthServerException;
import com.chaekchaek.auth.exception.InvalidAppleAuthorizationException;
import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class AppleTokenClient {

    private final RestClient restClient;
    private final AppleAuthProperties properties;
    private final AppleClientSecretProvider clientSecretProvider;

    AppleTokenClient(
            RestClient restClient,
            AppleAuthProperties properties,
            AppleClientSecretProvider clientSecretProvider
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.clientSecretProvider = clientSecretProvider;
    }

    public AppleTokenResponse exchange(String authorizationCode) {
        MultiValueMap<String, String> form = commonForm();
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        try {
            AppleTokenResponse response = restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);
            if (response == null || response.refreshToken() == null || response.refreshToken().isBlank()) {
                throw new InvalidAppleAuthorizationException();
            }
            return response;
        } catch (HttpClientErrorException exception) {
            throw new InvalidAppleAuthorizationException(exception);
        } catch (RestClientException exception) {
            throw new AppleAuthServerException(exception);
        }
    }

    public void revoke(String refreshToken) {
        MultiValueMap<String, String> form = commonForm();
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        try {
            restClient.post()
                    .uri(properties.revokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new AppleAuthServerException(exception);
        }
    }

    private MultiValueMap<String, String> commonForm() {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", properties.clientId());
        form.add("client_secret", clientSecretProvider.create());
        return form;
    }
}
