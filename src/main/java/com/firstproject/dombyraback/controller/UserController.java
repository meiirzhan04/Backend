package com.firstproject.dombyraback.controller;

import com.firstproject.dombyraback.auth.UserDto;
import com.firstproject.dombyraback.repository.UserRepository;
import com.firstproject.dombyraback.security.JwtService;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api")
public class UserController {

    private final UserRepository userRepository;
    private final JwtService jwtService;

    public UserController(UserRepository userRepository, JwtService jwtService) {
        this.userRepository = userRepository;
        this.jwtService = jwtService;
    }

    @GetMapping("/users")
    public List<User> getAllUsers() {
        return userRepository.findAll();
    }

    @GetMapping("/profile")
    public ResponseEntity<UserDto> getProfile(Authentication authentication) {
        String phone = (String) authentication.getPrincipal();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        UserDto userDto = new UserDto(user.getId(), user.getPhone(), user.getName());
        return ResponseEntity.ok(userDto);
    }

    @PutMapping("/profile")
    public ResponseEntity<UserDto> updateProfile(
            Authentication authentication,
            @RequestBody UserDto userDto
    ) {
        String phone = (String) authentication.getPrincipal();
        User user = userRepository.findByPhone(phone)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (userDto.getPhone() != null) user.setPhone(userDto.getPhone());
        if (userDto.getName() != null) user.setName(userDto.getName());

        userRepository.save(user);

        UserDto updatedUser = new UserDto(user.getId(), user.getPhone(), user.getName());
        return ResponseEntity.ok(updatedUser);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout() {
        return ResponseEntity.ok().build();
    }
}