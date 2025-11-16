package com.merigaumata.apigateway.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpHeaders;
import org.springframework.security.authentication.AbstractAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.reactive.EnableWebFluxSecurity;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.ReactiveSecurityContextHolder;
import org.springframework.security.core.context.SecurityContextImpl;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.NimbusReactiveJwtDecoder;
import org.springframework.security.oauth2.jwt.ReactiveJwtDecoder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.security.oauth2.server.resource.introspection.NimbusReactiveOpaqueTokenIntrospector;
import org.springframework.security.oauth2.server.resource.introspection.ReactiveOpaqueTokenIntrospector;
import org.springframework.security.web.server.SecurityWebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

@Configuration
@EnableWebFluxSecurity
public class SecurityConfig {

    @Value("${spring.security.oauth2.resourceserver.jwt.jwk-set-uri:}")
    private String jwkSetUri;

    @Value("${spring.security.oauth2.resourceserver.opaque.introspection-uri:http://localhost:9000/oauth2/introspect}")
    private String introspectionUri;

    @Value("${spring.security.oauth2.resourceserver.opaque.client-id:client}")
    private String clientId;

    @Value("${spring.security.oauth2.resourceserver.opaque.client-secret:secret}")
    private String clientSecret;

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        // We rely mostly on our custom WebFilter for token handling but still enable basic security features.
        http
                .csrf(ServerHttpSecurity.CsrfSpec::disable)
                .cors(Customizer.withDefaults())
                .authorizeExchange(exchanges -> exchanges
                        .pathMatchers("/api/auth/login","/actuator/**", "/actuator", "/favicon.ico", "/v3/api-docs/**", "/swagger-ui/**", "/fallback/**").permitAll()
                        .anyExchange().authenticated()
                )
                .oauth2ResourceServer(oauth -> oauth
                        .jwt(Customizer.withDefaults())
                );

        return http.build();
    }

    @Bean
    public WebFilter tokenAuthenticationWebFilter() {
        // Create decoder and introspector instances directly to avoid circular dependency
        ReactiveJwtDecoder jwtDecoder = jwkSetUri != null && !jwkSetUri.isEmpty()
                ? NimbusReactiveJwtDecoder.withJwkSetUri(jwkSetUri).build()
                : null;
        ReactiveOpaqueTokenIntrospector introspector = new NimbusReactiveOpaqueTokenIntrospector(
                introspectionUri, clientId, clientSecret);

        return (ServerWebExchange exchange, WebFilterChain chain) -> {
            String authHeader = exchange.getRequest().getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                return chain.filter(exchange); // no token – depending on paths security may reject later
            }
            String token = authHeader.substring(7);

            // Try JWT decode first — it's faster when token is encoded as JWT
            Mono<AbstractAuthenticationToken> authMono;

            if (jwtDecoder != null) {
                authMono = jwtDecoder.decode(token)
                        .map(jwt -> {
                            Collection<SimpleGrantedAuthority> authorities = extractAuthoritiesFromJwt(jwt);
                            return (AbstractAuthenticationToken) new JwtAuthenticationToken(jwt, authorities, jwt.getSubject());
                        })
                        .onErrorResume(jwtEx -> {
                            // If JWT decoding fails, try introspection (opaque token)
                            return introspector.introspect(token)
                                    .map(principal -> {
                                        Collection<SimpleGrantedAuthority> authorities = extractAuthoritiesFromPrincipal(principal);
                                        // using UsernamePasswordAuthenticationToken as a generic token
                                        return (AbstractAuthenticationToken) new UsernamePasswordAuthenticationToken(principal.getName(), token, authorities);
                                    });
                        });
            } else {
                // No JWT decoder configured, use introspection directly
                authMono = introspector.introspect(token)
                        .map(principal -> {
                            Collection<SimpleGrantedAuthority> authorities = extractAuthoritiesFromPrincipal(principal);
                            return (AbstractAuthenticationToken) new UsernamePasswordAuthenticationToken(principal.getName(), token, authorities);
                        });
            }

            return authMono.flatMap(authentication -> {
                SecurityContextImpl context = new SecurityContextImpl(authentication);
                // Put user details into exchange attributes (also used by downstream header appender filter)
                exchange.getAttributes().put("authenticatedPrincipal", authentication);
                return chain.filter(exchange)
                        .contextWrite(ReactiveSecurityContextHolder.withSecurityContext(Mono.just(context)));
            }).onErrorResume(ex -> {
                exchange.getResponse().setStatusCode(org.springframework.http.HttpStatus.UNAUTHORIZED);
                return exchange.getResponse().setComplete();
            });
        };
    }

    private Collection<SimpleGrantedAuthority> extractAuthoritiesFromJwt(Jwt jwt) {
        Set<SimpleGrantedAuthority> auth = new HashSet<>();
        Object scope = jwt.getClaims().getOrDefault("scope", jwt.getClaims().get("scp"));
        if (scope instanceof String) {
            String[] scopes = ((String) scope).split(" ");
            for (String s : scopes) auth.add(new SimpleGrantedAuthority("SCOPE_" + s));
        } else if (scope instanceof Collection) {
            for (Object s : (Collection<?>) scope) auth.add(new SimpleGrantedAuthority("SCOPE_" + s.toString()));
        }
        return auth;
    }

    private Collection<SimpleGrantedAuthority> extractAuthoritiesFromPrincipal(org.springframework.security.oauth2.core.OAuth2AuthenticatedPrincipal principal) {
        Set<SimpleGrantedAuthority> auth = new HashSet<>();
        Object scope = principal.getAttribute("scope");
        if (scope instanceof String) {
            for (String s : ((String) scope).split(" ")) auth.add(new SimpleGrantedAuthority("SCOPE_" + s));
        } else if (scope instanceof Collection) {
            for (Object s : (Collection<?>) scope) auth.add(new SimpleGrantedAuthority("SCOPE_" + s.toString()));
        }
        return auth;
    }

}
