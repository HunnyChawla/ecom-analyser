package com.ecomanalyser.service;

import com.ecomanalyser.config.JwtUtil;
import com.ecomanalyser.domain.UserEntity;
import com.ecomanalyser.dto.AuthRequest;
import com.ecomanalyser.dto.AuthResponse;
import com.ecomanalyser.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {
    
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;
    private final AuthenticationManager authenticationManager;
    
    public AuthResponse signup(AuthRequest request) {
        // Check if user already exists
        if (userRepository.existsByEmail(request.getEmail())) {
            return new AuthResponse(null, null, null, null, null, null, "User already exists with this email", false);
        }
        
        // Check if GST number already exists
        if (userRepository.existsByGstNumber(request.getGstNumber())) {
            return new AuthResponse(null, null, null, null, null, null, "GST number already registered", false);
        }
        
        // Create new user
        UserEntity user = new UserEntity();
        user.setEmail(request.getEmail());
        user.setPassword(passwordEncoder.encode(request.getPassword()));
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setGstNumber(request.getGstNumber());
        user.setRole(UserEntity.Role.USER);
        user.setEnabled(true);
        
        UserEntity savedUser = userRepository.save(user);
        
        // Generate token
        String token = jwtUtil.generateToken(savedUser);
        
        return new AuthResponse(
            token,
            savedUser.getEmail(),
            savedUser.getFirstName(),
            savedUser.getLastName(),
            savedUser.getGstNumber(),
            savedUser.getRole().name(),
            "User registered successfully",
            true
        );
    }
    
    public AuthResponse login(AuthRequest request) {
        try {
            // Authenticate user
            authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
            );
            
            // Load user details
            UserDetails userDetails = userRepository.findByEmail(request.getEmail())
                    .orElseThrow(() -> new RuntimeException("User not found"));
            
            UserEntity user = (UserEntity) userDetails;
            
            // Generate token
            String token = jwtUtil.generateToken(userDetails);
            
            return new AuthResponse(
                token,
                user.getEmail(),
                user.getFirstName(),
                user.getLastName(),
                user.getGstNumber(),
                user.getRole().name(),
                "Login successful",
                true
            );
            
        } catch (Exception e) {
            return new AuthResponse(null, null, null, null, null, null, "Invalid email or password", false);
        }
    }
}
