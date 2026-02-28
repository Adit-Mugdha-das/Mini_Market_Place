package com.example.mini_marketplace.service;

import com.example.mini_marketplace.entity.Role;
import com.example.mini_marketplace.entity.User;
import com.example.mini_marketplace.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("CustomUserDetailsService Unit Tests")
class CustomUserDetailsServiceTest {

    @Mock private UserRepository userRepository;

    @InjectMocks
    private CustomUserDetailsService service;

    private User buyerUser;
    private User sellerUser;
    private User disabledUser;

    @BeforeEach
    void setUp() {
        Role buyerRole = new Role(Role.RoleName.ROLE_BUYER);
        Role sellerRole = new Role(Role.RoleName.ROLE_SELLER);

        buyerUser = new User();
        buyerUser.setId(1L);
        buyerUser.setUsername("buyer1");
        buyerUser.setPassword("$2a$ENCODED_PASS");
        buyerUser.setEnabled(true);
        buyerUser.setRoles(Set.of(buyerRole));

        sellerUser = new User();
        sellerUser.setId(2L);
        sellerUser.setUsername("seller1");
        sellerUser.setPassword("$2a$ENCODED_PASS");
        sellerUser.setEnabled(true);
        sellerUser.setRoles(Set.of(sellerRole));

        disabledUser = new User();
        disabledUser.setId(3L);
        disabledUser.setUsername("disabled1");
        disabledUser.setPassword("$2a$ENCODED_PASS");
        disabledUser.setEnabled(false);
        disabledUser.setRoles(Set.of(buyerRole));
    }

    // ─── loadUserByUsername: success ─────────────────────────────────────────

    @Test
    @DisplayName("loadUserByUsername — returns correct username")
    void loadUser_returnsCorrectUsername() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyerUser));

        UserDetails details = service.loadUserByUsername("buyer1");

        assertThat(details.getUsername()).isEqualTo("buyer1");
    }

    @Test
    @DisplayName("loadUserByUsername — returns encoded password unchanged")
    void loadUser_returnsEncodedPassword() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyerUser));

        UserDetails details = service.loadUserByUsername("buyer1");

        assertThat(details.getPassword()).isEqualTo("$2a$ENCODED_PASS");
    }

    @Test
    @DisplayName("loadUserByUsername — buyer gets ROLE_BUYER authority")
    void loadUser_buyer_hasCorrectAuthority() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyerUser));

        UserDetails details = service.loadUserByUsername("buyer1");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_BUYER");
    }

    @Test
    @DisplayName("loadUserByUsername — seller gets ROLE_SELLER authority")
    void loadUser_seller_hasCorrectAuthority() {
        when(userRepository.findByUsername("seller1")).thenReturn(Optional.of(sellerUser));

        UserDetails details = service.loadUserByUsername("seller1");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_SELLER");
    }

    @Test
    @DisplayName("loadUserByUsername — user with admin role gets ROLE_ADMIN authority")
    void loadUser_admin_hasCorrectAuthority() {
        Role adminRole = new Role(Role.RoleName.ROLE_ADMIN);
        User adminUser = new User();
        adminUser.setId(4L);
        adminUser.setUsername("admin");
        adminUser.setPassword("$2a$ENCODED");
        adminUser.setEnabled(true);
        adminUser.setRoles(Set.of(adminRole));

        when(userRepository.findByUsername("admin")).thenReturn(Optional.of(adminUser));

        UserDetails details = service.loadUserByUsername("admin");

        assertThat(details.getAuthorities())
                .extracting(GrantedAuthority::getAuthority)
                .containsExactly("ROLE_ADMIN");
    }

    @Test
    @DisplayName("loadUserByUsername — disabled user isEnabled returns false")
    void loadUser_disabledUser_isNotEnabled() {
        when(userRepository.findByUsername("disabled1")).thenReturn(Optional.of(disabledUser));

        UserDetails details = service.loadUserByUsername("disabled1");

        assertThat(details.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("loadUserByUsername — enabled user isEnabled returns true")
    void loadUser_enabledUser_isEnabled() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyerUser));

        UserDetails details = service.loadUserByUsername("buyer1");

        assertThat(details.isEnabled()).isTrue();
    }

    @Test
    @DisplayName("loadUserByUsername — account flags are all non-expired and non-locked")
    void loadUser_accountFlags_areAllTrue() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyerUser));

        UserDetails details = service.loadUserByUsername("buyer1");

        assertThat(details.isAccountNonExpired()).isTrue();
        assertThat(details.isAccountNonLocked()).isTrue();
        assertThat(details.isCredentialsNonExpired()).isTrue();
    }

    // ─── loadUserByUsername: failure ──────────────────────────────────────────

    @Test
    @DisplayName("loadUserByUsername — throws UsernameNotFoundException when user not found")
    void loadUser_throws_whenUserNotFound() {
        when(userRepository.findByUsername("ghost")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.loadUserByUsername("ghost"))
                .isInstanceOf(UsernameNotFoundException.class)
                .hasMessageContaining("User not found");
    }

    @Test
    @DisplayName("loadUserByUsername — queries repository exactly once")
    void loadUser_queriesRepositoryOnce() {
        when(userRepository.findByUsername("buyer1")).thenReturn(Optional.of(buyerUser));

        service.loadUserByUsername("buyer1");

        verify(userRepository, times(1)).findByUsername("buyer1");
    }
}
