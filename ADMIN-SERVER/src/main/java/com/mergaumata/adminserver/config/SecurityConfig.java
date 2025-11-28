package com.mergaumata.adminserver.config;

import de.codecentric.boot.admin.server.config.AdminServerProperties;
import javax.sql.DataSource;

import org.springframework.boot.actuate.autoconfigure.security.servlet.EndpointRequest;
import org.springframework.boot.actuate.health.HealthEndpoint;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.Customizer;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity;
import org.springframework.security.config.annotation.web.configurers.HeadersConfigurer;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.JdbcUserDetailsManager;
import org.springframework.security.provisioning.UserDetailsManager;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.SavedRequestAwareAuthenticationSuccessHandler;
import org.springframework.security.web.csrf.CookieCsrfTokenRepository;

@Configuration
@EnableWebSecurity
public class SecurityConfig {

  private final AdminServerProperties adminServer;

  public SecurityConfig(AdminServerProperties adminServer) {
    this.adminServer = adminServer;
  }

  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http) throws Exception {

    SavedRequestAwareAuthenticationSuccessHandler successHandler =
        new SavedRequestAwareAuthenticationSuccessHandler();
    successHandler.setTargetUrlParameter("redirectTo");
    successHandler.setDefaultTargetUrl(adminServer.path("/"));

    http.authorizeHttpRequests(
            auth ->
                auth.requestMatchers(
                        adminServer.path("/assets/**"),
                        adminServer.path("/login"),
                        adminServer.path("/actuator/health/**"),
                        adminServer.path("/actuator/**"))
                    .permitAll()
                    .anyRequest()
                    .authenticated())
        .formLogin(
            form ->
                form.loginPage(adminServer.path("/login"))
                    .successHandler(successHandler)
                    .permitAll())
        .logout(
            logout ->
                logout
                    .logoutUrl(adminServer.path("/logout"))
                    .logoutSuccessUrl(adminServer.path("/login?logout"))
                    .permitAll())
        .httpBasic(Customizer.withDefaults())
        .headers(headers -> headers.frameOptions(HeadersConfigurer.FrameOptionsConfig::sameOrigin))
        .csrf(
            csrf ->
                csrf.csrfTokenRepository(CookieCsrfTokenRepository.withHttpOnlyFalse())
                    .ignoringRequestMatchers(
                        adminServer.path("/instances"),
                        adminServer.path("/instances/**"),
                        adminServer.path("/actuator/**")));
    return http.build();
  }

  @Bean
  public UserDetailsManager userDetailsManager(DataSource dataSource) {
      return new JdbcUserDetailsManager(dataSource);
  }

  @Bean
  public PasswordEncoder passwordEncoder() {
      return new BCryptPasswordEncoder();
  }
}
