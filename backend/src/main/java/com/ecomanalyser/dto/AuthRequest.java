package com.ecomanalyser.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class AuthRequest {
    
    @NotBlank(message = "Email is required")
    @Email(message = "Please provide a valid email address")
    private String email;
    
    @NotBlank(message = "Password is required")
    @Size(min = 6, message = "Password must be at least 6 characters long")
    private String password;
    
    // For signup
    private String firstName;
    private String lastName;
    
    @NotBlank(message = "GST number is required")
    @Size(min = 15, max = 15, message = "GST number must be exactly 15 characters")
    private String gstNumber;
}
