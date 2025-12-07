package com.merigaumata.auth.service;

import com.merigaumata.user.api.UsersApi;
import com.merigaumata.user.model.UserResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import org.springframework.web.reactive.function.client.WebClient;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CustomUserDetailsService implements UserDetailsService {

    private final UsersApi usersApi;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        // Call UserService to get user details

        UserResponse userResponse = usersApi.getUserByUsername(username).block();

            if (Objects.nonNull(userResponse)) {
                throw new UsernameNotFoundException("User not found: " + username);
            }

//            String password = userResponse.getPassword();
            List<String> roles = ObjectUtils.isEmpty(userResponse.getRoles()) ? List.of("ROLE_USER") : userResponse.getRoles();

        boolean enabled = !ObjectUtils.isEmpty(userResponse.getEnabled()) ? userResponse.getEnabled() : true;
        List<SimpleGrantedAuthority> authorities = roles.stream()
                    .map(SimpleGrantedAuthority::new)
                    .toList();

            return User.builder()
                    .username(username)
//                    .password(password)
                    .authorities(authorities)
                    .accountExpired(false)
                    .accountLocked(false)
                    .credentialsExpired(false)
                    .disabled(!enabled)
                    .build();
    }
}