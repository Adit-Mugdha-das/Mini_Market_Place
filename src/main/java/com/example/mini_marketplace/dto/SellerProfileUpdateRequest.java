package com.example.mini_marketplace.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class SellerProfileUpdateRequest {

    @NotBlank(message = "Full name is required")
    @Size(max = 120, message = "Full name must be at most 120 characters")
    private String fullName;

    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    @Size(max = 120, message = "Email must be at most 120 characters")
    private String email;

    @Pattern(
            regexp = "^$|^[+0-9()\\-\\s]{7,25}$",
            message = "Phone number must be 7-25 characters and can include +, digits, spaces, (), -"
    )
    private String phoneNumber;

    @Size(max = 500, message = "Address must be at most 500 characters")
    private String address;
}
