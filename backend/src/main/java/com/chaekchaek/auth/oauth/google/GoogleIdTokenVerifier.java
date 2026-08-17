package com.chaekchaek.auth.oauth.google;

import com.chaekchaek.auth.exception.InvalidGoogleIdTokenException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

public class GoogleIdTokenVerifier {

    private final JwtDecoder jwtDecoder;

    GoogleIdTokenVerifier(JwtDecoder jwtDecoder) {
        this.jwtDecoder = jwtDecoder;
    }

    public GoogleProfile verify(String idToken) {
        try {
            Jwt jwt = jwtDecoder.decode(idToken);
            validateSubject(jwt);

            return new GoogleProfile(
                    jwt.getSubject(),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("picture")
            );
        } catch (JwtException exception) {
            throw new InvalidGoogleIdTokenException(exception);
        }
    }

    private void validateSubject(Jwt jwt) {
        if (jwt.getSubject() == null || jwt.getSubject().isBlank()) {
            throw new InvalidGoogleIdTokenException();
        }
    }
}
