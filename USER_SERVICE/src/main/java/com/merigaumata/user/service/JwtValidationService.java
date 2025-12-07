package com.merigaumata.user.service;

import com.merigaumata.auth.api.DiscoveryApi;
import com.nimbusds.jose.JWSVerifier;
import com.nimbusds.jose.crypto.RSASSAVerifier;
import com.nimbusds.jose.jwk.JWK;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jwt.JWTClaimsSet;
import com.nimbusds.jwt.SignedJWT;
import java.text.ParseException;
import java.time.Instant;
import java.util.Date;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Slf4j
@Service
public class JwtValidationService {

  private final DiscoveryApi discoveryApi;

  @Value("${auth.jwks-uri:http://auth-service:8081/auth/.well-known/jwks.json}")
  private String jwksUri;

  public JwtValidationService(DiscoveryApi discoveryApi) {
    this.discoveryApi = discoveryApi;
  }

  /** Validate JWT token by verifying signature and checking expiration */
  public JWTClaimsSet validateToken(String token) throws Exception {
    SignedJWT signedJWT = SignedJWT.parse(token);
    String keyId = signedJWT.getHeader().getKeyID();

    // Get public key from JWKS endpoint (cached)
    JWK jwk = getJWK(keyId);
    if (jwk == null) {
      throw new RuntimeException("Public key not found for kid: " + keyId);
    }

    RSAKey rsaKey = (RSAKey) jwk;
    JWSVerifier verifier = new RSASSAVerifier(rsaKey);

    // Verify signature
    if (!signedJWT.verify(verifier)) {
      throw new RuntimeException("Invalid JWT signature");
    }

    JWTClaimsSet claims = signedJWT.getJWTClaimsSet();

    // Validate expiration
    Date expirationTime = claims.getExpirationTime();
    if (expirationTime == null || expirationTime.before(Date.from(Instant.now()))) {
      throw new RuntimeException("Token expired");
    }

    // Validate audience (optional but recommended)
    String audience = claims.getAudience().get(0);
    if (!"user-service".equals(audience)) {
      log.warn("Token audience mismatch: expected=user-service, actual={}", audience);
    }

    return claims;
  }

  /** Fetch public key from JWKS endpoint Cached for 5 minutes to reduce load on AuthService */
  @Cacheable(value = "jwks", key = "#keyId")
  public JWK getJWK(String keyId) throws ParseException {
    Map<String, Object> response = discoveryApi.getJwks().block();

    JWKSet jwkSet = JWKSet.parse(response);
    return jwkSet.getKeyByKeyId(keyId);
  }
}
