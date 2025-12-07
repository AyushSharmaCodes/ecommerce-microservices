package com.merigaumata.user.config;

import com.merigaumata.auth.ApiClient;
import com.merigaumata.auth.api.DiscoveryApi;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.stereotype.Component;

@Configuration
@Component
public class ApiConfig {

    @NotBlank
    @NotNull
//    @Value("${app.service.apis.auth-url}")
    @Value("http://localhost:8081")
    private String userServiceUrl;

    @Bean
    public DiscoveryApi userServiceApi(){
        ApiClient authApi = new ApiClient();
        authApi.setBasePath(userServiceUrl);
        return new DiscoveryApi(authApi);
    }
}