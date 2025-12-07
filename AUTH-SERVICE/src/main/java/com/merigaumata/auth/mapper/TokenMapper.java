package com.merigaumata.auth.mapper;

import com.merigaumata.auth.model.LoginResponse;
import com.merigaumata.auth.model.TokenIntrospectionResponse;
import com.nimbusds.jwt.JWTClaimsSet;
import org.mapstruct.*;

import java.util.Date;
import java.util.List;

@Mapper(
        componentModel = "spring",
        unmappedTargetPolicy = ReportingPolicy.IGNORE,
        injectionStrategy = InjectionStrategy.CONSTRUCTOR
)
public interface TokenMapper {

    /**
     * Create LoginResponse from token details
     */
    @Mapping(target = "accessToken", source = "accessToken")
    @Mapping(target = "refreshToken", source = "refreshToken")
    @Mapping(target = "tokenType", constant = "Bearer")
    @Mapping(target = "expiresIn", source = "expiresIn")
    @Mapping(target = "userId", source = "userId")
    @Mapping(target = "roles", source = "roles")
    @Mapping(target = "scopes", source = "scopes")
    LoginResponse toLoginResponse(
            String accessToken,
            String refreshToken,
            long expiresIn,
            String userId,
            List<String> roles,
            List<String> scopes
    );

    /**
     * Map JWT claims to TokenIntrospectionResponse
     */
    @Mapping(target = "active", constant = "true")
    @Mapping(target = "subject", source = "subject")
    @Mapping(target = "issuer", source = "issuer")
    @Mapping(target = "expiration", source = "expirationTime", qualifiedByName = "dateToTimestamp")
    @Mapping(target = "roles", expression = "java(extractRoles(claims))")
    @Mapping(target = "scopes", expression = "java(extractScopes(claims))")
    TokenIntrospectionResponse toIntrospectionResponse(JWTClaimsSet claims);

    /**
     * Create inactive token introspection response
     */
    default TokenIntrospectionResponse toInactiveIntrospectionResponse() {
        return new TokenIntrospectionResponse(false, null, null, null, null, null);
    }

    /**
     * Convert Date to Unix timestamp
     */
    @Named("dateToTimestamp")
    default Long dateToTimestamp(Date date) {
        return date != null ? date.getTime() / 1000 : null;
    }

    /**
     * Extract roles from JWT claims
     */
    @SuppressWarnings("unchecked")
    default List<String> extractRoles(JWTClaimsSet claims) {
        try {
            return (List<String>) claims.getClaim("roles");
        } catch (Exception e) {
            return List.of();
        }
    }

    /**
     * Extract scopes from JWT claims
     */
    @SuppressWarnings("unchecked")
    default List<String> extractScopes(JWTClaimsSet claims) {
        try {
            return (List<String>) claims.getClaim("scopes");
        } catch (Exception e) {
            return List.of();
        }
    }
}
