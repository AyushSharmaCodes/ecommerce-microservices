package com.merigaumata.auth.security;

import com.nimbusds.jose.JWSAlgorithm;
import com.nimbusds.jose.JWSHeader;
import com.nimbusds.jose.JWSSigner;
import com.nimbusds.jose.crypto.RSASSASigner;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.security.KeyPair;
import java.security.interfaces.RSAPrivateKey;
import java.time.Instant;
import java.util.Date;
import java.util.List;
import java.util.UUID;

@Slf4j
@Getter
@Component
@RequiredArgsConstructor
public class JwtTokenProvider {

    private final KeyPair keyPair;

    @Value("${jwt.access-token-validity:900}") // 15 minutes
    private long accessTokenValidity;

    @Value("${jwt.refresh-token-validity:2592000}") // 30 days
    private long refreshTokenValidity;

    @Value("${jwt.issuer:auth-service}")
    private String issuer;

    @Value("${jwt.key-id:main-key}")
    private String keyId;

    public String generateAccessToken(String userId, List<String> roles, List<String> scopes, String audience) {
        try {
            Instant now = Instant.now();
            Instant expiration = now.plusSeconds(accessTokenValidity);

            JWTClaimsSet claimsSet = new JWTClaimsSet.Builder()
                    .subject(userId)
                    .issuer(issuer)
                    .audience(audience)
                    .expirationTime(Date.from(expiration))
                    .issueTime(Date.from(now))
                    .jwtID(UUID.randomUUID().toString())
                    .claim("roles", roles)
                    .claim("scopes", scopes)
                    .claim("token_type", "access")
                    .build();

            SignedJWT signedJWT = new SignedJWT(
                    new JWSHeader.Builder(JWSAlgorithm.RS256).keyID(keyId).build(),
                    claimsSet
            );

            RSAPrivateKey privateKey = (RSAPrivateKey) keyPair.getPrivate();
            JWSSigner signer = new RSASSASigner(privateKey);
            signedJWT.sign(signer);

            return signedJWT.serialize();
        } catch (Exception e) {
            log.error("Error generating access token", e);
            throw new RuntimeException("Failed to generate access token", e);
        }
    }

    public String generateRefreshToken(String userId) {
        return UUID.randomUUID().toString().replace("-", "") +
                UUID.randomUUID().toString().replace("-", "");
    }
}
