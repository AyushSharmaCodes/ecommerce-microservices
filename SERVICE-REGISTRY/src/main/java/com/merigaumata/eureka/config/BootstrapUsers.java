package com.merigaumata.eureka.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;

@Configuration
public class BootstrapUsers implements CommandLineRunner {

    Logger log = LoggerFactory.getLogger(this.getClass());

    private final UserDetailsManager users;
    private final PasswordEncoder passwordEncoder;

    @Value("${EUREKA_ADMIN_USER:}")
    private String adminUser;

    @Value("${EUREKA_ADMIN_PASS:}")
    private String adminPass;

    @Value("${EUREKA_SERVICE_USER:}")
    private String serviceUser;

    @Value("${EUREKA_SERVICE_PASS:}")
    private String servicePass;

    public BootstrapUsers(UserDetailsManager users, PasswordEncoder passwordEncoder) {
        this.users = users;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) throws Exception {
        // Only create users if env vars are provided — prevents accidental creation in dev when not intended
        if (adminUser != null && !adminUser.isBlank() && adminPass != null && !adminPass.isBlank()) {
            if (!users.userExists(adminUser)) {
                UserDetails admin = User.withUsername(adminUser)
                        .password(passwordEncoder.encode(adminPass))
                        .roles("ADMIN")
                        .build();
                users.createUser(admin);
                log.info("Created default admin user: {}", adminUser);
            }
            log.info("adminUser or adminPass already exists, skipping creation.");
        }

        if (serviceUser != null && !serviceUser.isBlank() && servicePass != null && !servicePass.isBlank()) {
            if (!users.userExists(serviceUser)) {
                UserDetails svc = User.withUsername(serviceUser)
                        .password(passwordEncoder.encode(servicePass))
                        .roles("SERVICE")
                        .build();
                users.createUser(svc);
                log.info("Created default service user: {}", serviceUser);
            }
            log.info("serviceUser or servicePass already exists, skipping creation.");
        }
    }
}