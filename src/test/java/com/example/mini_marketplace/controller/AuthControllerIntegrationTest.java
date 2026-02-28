package com.example.mini_marketplace.controller;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
@DisplayName("AuthController Integration Tests")
class AuthControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    // ─── GETauth/login ──────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/login — returns 200 and renders login view")
    void getLogin_returns200_andRendersLoginView() throws Exception {
        mockMvc.perform(get("/auth/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"));
    }

    @Test
    @DisplayName("GET /auth/login?error — adds errorMessage to model")
    void getLogin_withError_addsErrorMessage() throws Exception {
        mockMvc.perform(get("/auth/login").param("error", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("GET /auth/login?logout — adds logoutMessage to model")
    void getLogin_withLogout_addsLogoutMessage() throws Exception {
        mockMvc.perform(get("/auth/login").param("logout", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/login"))
                .andExpect(model().attributeExists("logoutMessage"));
    }

    // ─── GET /auth/register ───────────────────────────────────────────────────

    @Test
    @DisplayName("GET /auth/register — returns 200 and puts registerRequest in model")
    void getRegister_returns200_andAddsRegisterRequest() throws Exception {
        mockMvc.perform(get("/auth/register"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("registerRequest"));
    }

    // ─── POST /auth/register ──────────────────────────────────────────────────

    @Test
    @DisplayName("POST /auth/register — valid data redirects to login with success message")
    void postRegister_validData_redirectsToLogin() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName",        "Test User")
                        .param("username",        "testuser_int")
                        .param("email",           "testuser_int@example.com")
                        .param("password",        "password123")
                        .param("confirmPassword", "password123")
                        .param("role",            "BUYER"))
                .andExpect(status().is3xxRedirection())
                .andExpect(redirectedUrl("/auth/login"));
    }

    @Test
    @DisplayName("POST /auth/register — blank fields stay on register view with errors")
    void postRegister_blankFields_staysOnRegisterView() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName",        "")
                        .param("username",        "")
                        .param("email",           "")
                        .param("password",        "")
                        .param("confirmPassword", ""))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }

    @Test
    @DisplayName("POST /auth/register — password mismatch stays on register view with error")
    void postRegister_passwordMismatch_staysOnRegisterView() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName",        "Mismatch User")
                        .param("username",        "mismatch_user")
                        .param("email",           "mismatch@example.com")
                        .param("password",        "password123")
                        .param("confirmPassword", "different456")
                        .param("role",            "BUYER"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("POST /auth/register — duplicate username stays on register view with error")
    void postRegister_duplicateUsername_staysOnRegisterView() throws Exception {
        // Register once
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName",        "First User")
                        .param("username",        "dupuser")
                        .param("email",           "first@example.com")
                        .param("password",        "password123")
                        .param("confirmPassword", "password123")
                        .param("role",            "BUYER"))
                .andExpect(status().is3xxRedirection());

        // Try same username again
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName",        "Second User")
                        .param("username",        "dupuser")
                        .param("email",           "second@example.com")
                        .param("password",        "password123")
                        .param("confirmPassword", "password123")
                        .param("role",            "BUYER"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().attributeExists("errorMessage"));
    }

    @Test
    @DisplayName("POST /auth/register — invalid email format stays on register view")
    void postRegister_invalidEmail_staysOnRegisterView() throws Exception {
        mockMvc.perform(post("/auth/register")
                        .with(csrf())
                        .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                        .param("fullName",        "Bad Email User")
                        .param("username",        "bademail_user")
                        .param("email",           "not-an-email")
                        .param("password",        "password123")
                        .param("confirmPassword", "password123")
                        .param("role",            "BUYER"))
                .andExpect(status().isOk())
                .andExpect(view().name("auth/register"))
                .andExpect(model().hasErrors());
    }
}
