package com.merigaumata.auth.config;

import com.merigaumata.user.ApiClient;
import com.merigaumata.user.api.UsersApi;
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
//    @Value("${app.service.apis.user-url}")
    @Value("http://localhost:8082")
    private String userServiceUrl;

    @Bean
    public UsersApi userServiceApi(){
        ApiClient userApiClient = new ApiClient();
        userApiClient.setBasePath(userServiceUrl);
        return new UsersApi(userApiClient);
    }
}
