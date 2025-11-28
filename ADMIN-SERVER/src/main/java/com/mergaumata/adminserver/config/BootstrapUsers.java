package com.mergaumata.adminserver.config;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.provisioning.UserDetailsManager;
import org.thymeleaf.util.StringUtils;

@Component
public class BootstrapUsers implements CommandLineRunner {

  // 🔑 1. AUDIT LOGGER: Must be manually declared using the exact name
  // configured in logback-spring.xml to route the logs correctly.
  private static final Logger AUDIT_LOGGER = LoggerFactory.getLogger("com.mergaumata.adminserver");

  private final UserDetailsManager users;
  private final PasswordEncoder passwordEncoder;

  @Value("${ADMIN_USER:}")
  private String adminUser;

  @Value("${ADMIN_PASS:}")
  private String adminPass;

  @Value("${USER_USER:}")
  private String user;

  @Value("${USER_PASS:}")
  private String pass;

  public BootstrapUsers(UserDetailsManager users, PasswordEncoder passwordEncoder) {
    this.users = users;
    this.passwordEncoder = passwordEncoder;
  }

  @Override
  public void run(String... args) throws Exception {
    // Only create users if env vars are provided — prevents accidental creation in dev when not
    // intended
    if (!StringUtils.isEmptyOrWhitespace(adminUser)
        && !adminUser.isBlank()
        && !StringUtils.isEmptyOrWhitespace(adminPass)
        && !adminPass.isBlank()) {
      if (!users.userExists(adminUser)) {
        UserDetails admin =
            User.withUsername(adminUser)
                .password(passwordEncoder.encode(adminPass))
                .roles("ADMIN")
                .build();
        users.createUser(admin);
          AUDIT_LOGGER.info("Created default admin user: {}", adminUser);
      }
        AUDIT_LOGGER.info("adminUser or adminPass already exists, skipping creation.");
    }

    if (!StringUtils.isEmptyOrWhitespace(user)
        && !user.isBlank()
        && !StringUtils.isEmptyOrWhitespace(pass)
        && !pass.isBlank()) {
      if (!users.userExists(user)) {
        UserDetails svc =
            User.withUsername(user).password(passwordEncoder.encode(pass)).roles("USER").build();
        users.createUser(svc);
          AUDIT_LOGGER.info("Created default user: {}", user);
      }
        AUDIT_LOGGER.info("user or pass already exists, skipping creation.");
    }
  }
}
