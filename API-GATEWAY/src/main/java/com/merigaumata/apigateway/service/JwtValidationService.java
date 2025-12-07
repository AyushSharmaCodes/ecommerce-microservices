package com.merigaumata.apigateway.service;

import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.time.Instant;
import java.util.Date;

@Slf4j
@Service
public class JwtValidationService {

    private final WebClient webClient;
    private final String jwksUri;

    public JwtValidationService(WebClient.Builder webClientBuilder,
                                @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri}") String jwksUri) {
        this.webClient = webClientBuilder.build();
        this.jwksUri = jwksUri;
    }

    public Mono<JWTClaimsSet> validateToken(String token) {
        return Mono.fromCallable(() -> {
            SignedJWT signedJWT = SignedJWT.parse(token);
            String keyId = signedJWT.getHeader().getKeyID();

            // Get public key from JWKS
            JWK jwk = getJWK(keyId).block();
            if (jwk == null) {
                throw new RuntimeException("Public key not found for kid: " + keyId);
            }

            RSAKey rsaKey = (RSAKey) jwk;
            JWSVerifier verifier = new RSASSAVerifier(rsaKey);

            if (!signedJWT.verify(verifier)) {
                throw new RuntimeException("Invalid JWT signature");
            }

            JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

            // Validate expiration
            Date expirationTime = claims.getExpirationTime();
            if (expirationTime == null || expirationTime.before(Date.from(Instant.now()))) {
                throw new RuntimeException("Token expired");
            }

            return claims;
        });
    }

    @Cacheable(value = "jwks", key = "#keyId")
    public Mono<JWK> getJWK(String keyId) {
        return webClient.get()
                .uri(jwksUri)
                .retrieve()
                .bodyToMono(String.class)
                .map(response -> {
                    try {
                        JWKSet jwkSet = JWKSet.parse(response);
                        return jwkSet.getKeyByKeyId(keyId);
                    } catch (Exception e) {
                        throw new RuntimeException("Failed to parse JWKS", e);
                    }
                });
    }
}