package com.ecomanalyser.dto;

import lombok.Data;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class AuthResponse {
    
    private String token;
    private String email;
    private String firstName;
    private String lastName;
    private String role;
    private String message;
    private boolean success;
}
