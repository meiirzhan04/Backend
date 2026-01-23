package com.firstproject.dombyraback.auth;

import com.firstproject.dombyraback.auth.dto.LoginRequest;
import com.firstproject.dombyraback.auth.dto.RegisterRequest;
import com.firstproject.dombyraback.controller.User;
import com.firstproject.dombyraback.repository.UserRepository;
import com.firstproject.dombyraback.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.UNAUTHORIZED;

@Service
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(
            UserRepository userRepository,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    public AuthResponse register(RegisterRequest request) {

        if (!request.getPassword().equals(request.getConfirmPassword())) {
            throw new RuntimeException("Passwords do not match");
        }

        if (userRepository.existsByPhone(request.getPhone())) {
            throw new RuntimeException("Phone already registered");
        }

        User user = new User(
                request.getPhone(),
                passwordEncoder.encode(request.getPassword())
        );

        userRepository.save(user);
        String token = jwtService.generateToken(user.getPhone());
        return new AuthResponse(token);
    }

    public AuthResponse login(LoginRequest request) {

        User user = userRepository.findByPhone(request.getPhone())
                .orElseThrow(() ->
                        new ResponseStatusException(
                                BAD_REQUEST,
                                "User not found")
                );

        if (!passwordEncoder.matches(
                request.getPassword(),
                user.getPassword()
        )) {
            throw new ResponseStatusException(
                    UNAUTHORIZED,
                    "Wrong phone or password"
            );
        }

        String token = jwtService.generateToken(user.getPhone());

        return new AuthResponse(token);
    }
}