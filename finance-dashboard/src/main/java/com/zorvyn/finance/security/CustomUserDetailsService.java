package com.zorvyn.finance.security;

import com.zorvyn.finance.domain.entity.User;
import com.zorvyn.finance.domain.enums.UserStatus;
import com.zorvyn.finance.repository.UserRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UserDetailsService;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Service;

import java.util.Collections;

/**
 * Bridges our User entity to Spring Security's UserDetails.
 * Spring Security calls this during authentication to load the user from the DB.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

    @Autowired
    private UserRepository userRepository;

    @Override
    public UserDetails loadUserByUsername(String username) throws UsernameNotFoundException {
        User user = userRepository.findByUsername(username)
                .orElseThrow(() -> new UsernameNotFoundException("User not found: " + username));

        // inactive users can't authenticate — the "enabled" flag handles this
        boolean isActive = user.getStatus() == UserStatus.ACTIVE;

        return new org.springframework.security.core.userdetails.User(
                user.getUsername(),
                user.getPassword(),
                isActive, true, true, true,
                Collections.singletonList(
                        new SimpleGrantedAuthority("ROLE_" + user.getRole().getName().name())
                )
        );
    }
}
