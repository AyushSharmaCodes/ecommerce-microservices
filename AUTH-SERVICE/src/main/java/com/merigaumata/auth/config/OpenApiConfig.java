package com.merigaumata.auth.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeIn;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Contact;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.info.License;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.servers.Server;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(
        info = @Info(
                title = "Authentication Service API",
                version = "1.0.0",
                description = """
                        Production-grade Authentication Service providing secure JWT-based authentication 
                        with refresh token rotation, account lockout protection, and comprehensive security features.
                        
                        ## Features
                        - JWT token generation with RSA-256 signing
                        - Refresh token rotation (one-time use)
                        - Account lockout after failed attempts
                        - Password strength validation
                        - Token revocation and blacklisting
                        - JWKS endpoint for public key distribution
                        
                        ## Security
                        All endpoints except login, register, and JWKS require Bearer token authentication.
                        Tokens expire after 15 minutes and can be refreshed using the refresh token.
                        """,
                contact = @Contact(
                        name = "Platform Team",
                        email = "platform@enterprise.com",
                        url = "https://enterprise.com/support"
                ),
                license = @License(
                        name = "MIT License",
                        url = "https://opensource.org/licenses/MIT"
                )
        ),
        servers = {
                @Server(
                        description = "Production",
                        url = "https://api.enterprise.com"
                ),
                @Server(
                        description = "Staging",
                        url = "https://api-staging.enterprise.com"
                ),
                @Server(
                        description = "Local Development",
                        url = "http://localhost:8080"
                )
        },
        security = {
                @SecurityRequirement(name = "bearerAuth")
        }
)
@SecurityScheme(
        name = "bearerAuth",
        description = "JWT Bearer token authentication. Obtain token via /auth/login endpoint.",
        scheme = "bearer",
        type = SecuritySchemeType.HTTP,
        bearerFormat = "JWT",
        in = SecuritySchemeIn.HEADER
)
public class OpenApiConfig {
}