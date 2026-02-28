package com.example.mini_marketplace.service;

import com.example.mini_marketplace.dto.RegisterRequest;
import com.example.mini_marketplace.entity.Role;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.RoleRepository;
import com.example.mini_marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("AuthService Unit Tests")
class AuthServiceTest {

    @Mock private UserRepository  userRepository;
    @Mock private RoleRepository  roleRepository;
    @Mock private PasswordEncoder passwordEncoder;

    @InjectMocks
    private AuthService authService;

    private RegisterRequest request;

    @BeforeEach
    void setUp() {
        request = new RegisterRequest();
        request.setFullName("John Doe");
        request.setUsername("johndoe");
        request.setEmail("john@example.com");
        request.setPassword("secret123");
        request.setConfirmPassword("secret123");
        request.setRole("BUYER");
    }

    // ─── register: success paths ─────────────────────────────────────────────

    @Test
    @DisplayName("register — saves a new BUYER user with encoded password")
    void register_success_asBuyer() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        Role buyerRole = new Role(Role.RoleName.ROLE_BUYER);
        when(roleRepository.findByName(Role.RoleName.ROLE_BUYER)).thenReturn(Optional.of(buyerRole));

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        User saved = captor.getValue();

        assertThat(saved.getUsername()).isEqualTo("johndoe");
        assertThat(saved.getEmail()).isEqualTo("john@example.com");
        assertThat(saved.getFullName()).isEqualTo("John Doe");
        assertThat(saved.getPassword()).isEqualTo("ENCODED");
        assertThat(saved.getRoles()).extracting(Role::getName)
                .containsExactly(Role.RoleName.ROLE_BUYER);
    }

    @Test
    @DisplayName("register — saves a new SELLER user when role is SELLER")
    void register_success_asSeller() {
        request.setRole("SELLER");
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("ENCODED");
        Role sellerRole = new Role(Role.RoleName.ROLE_SELLER);
        when(roleRepository.findByName(Role.RoleName.ROLE_SELLER)).thenReturn(Optional.of(sellerRole));

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getRoles()).extracting(Role::getName)
                .containsExactly(Role.RoleName.ROLE_SELLER);
    }

    @Test
    @DisplayName("register — creates role if not found in repository")
    void register_createsRole_whenNotFound() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
        when(roleRepository.findByName(Role.RoleName.ROLE_BUYER)).thenReturn(Optional.empty());
        Role newRole = new Role(Role.RoleName.ROLE_BUYER);
        when(roleRepository.save(any(Role.class))).thenReturn(newRole);

        authService.register(request);

        verify(roleRepository).save(any(Role.class));
        verify(userRepository).save(any(User.class));
    }

    // ─── register: validation failures ───────────────────────────────────────

    @Test
    @DisplayName("register — throws when username is already taken")
    void register_throws_whenUsernameTaken() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Username is already taken");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — throws when email is already registered")
    void register_throws_whenEmailTaken() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Email is already registered");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — throws when passwords do not match")
    void register_throws_whenPasswordsMismatch() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        request.setConfirmPassword("DIFFERENT");

        assertThatThrownBy(() -> authService.register(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("Passwords do not match");

        verify(userRepository, never()).save(any());
    }

    @Test
    @DisplayName("register — never stores plain-text password")
    void register_passwordIsEncoded_notPlainText() {
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("$2a$HASHED");
        when(roleRepository.findByName(any())).thenReturn(Optional.of(new Role(Role.RoleName.ROLE_BUYER)));

        authService.register(request);

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPassword())
                .isNotEqualTo("secret123")
                .isEqualTo("$2a$HASHED");
    }

    @Test
    @DisplayName("register — unknown role defaults to BUYER")
    void register_unknownRole_defaultsToBuyer() {
        request.setRole("UNKNOWN_ROLE");
        when(userRepository.existsByUsername("johndoe")).thenReturn(false);
        when(userRepository.existsByEmail("john@example.com")).thenReturn(false);
        when(passwordEncoder.encode(anyString())).thenReturn("ENCODED");
        Role buyerRole = new Role(Role.RoleName.ROLE_BUYER);
        when(roleRepository.findByName(Role.RoleName.ROLE_BUYER)).thenReturn(Optional.of(buyerRole));

        authService.register(request);

        // Should resolve to BUYER, not SELLER
        verify(roleRepository, never()).findByName(Role.RoleName.ROLE_SELLER);
        verify(roleRepository).findByName(Role.RoleName.ROLE_BUYER);
    }
}
