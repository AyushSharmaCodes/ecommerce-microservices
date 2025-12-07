package com.merigaumata.auth.controller;

import com.merigaumata.auth.api.DiscoveryApi;
import com.nimbusds.jose.jwk.JWKSet;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequiredArgsConstructor
public class JwksController implements DiscoveryApi {

    private final JWKSet jwkSet;

    @Override
    public ResponseEntity<Map<String, Object>> getJwks() {
        return ResponseEntity.ok(jwkSet.toJSONObject());
    }

//    @GetMapping(value = "/jwks.json", produces = MediaType.APPLICATION_JSON_VALUE)
//    public Map<String, Object> getJwks() {
//        return jwkSet.toJSONObject();
//    }
}