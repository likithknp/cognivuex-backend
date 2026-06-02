package com.cognivuex.service;

import com.cognivuex.dto.LoginRequestDTO;
import com.cognivuex.dto.RegisterRequestDTO;
import com.cognivuex.entity.Role;
import com.cognivuex.entity.User;
import com.cognivuex.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public String register(RegisterRequestDTO request) {

        if(userRepository.findByEmail(request.getEmail()).isPresent()) {
            return "Email already exists";
        }

        User user = User.builder()
                .name(request.getName())
                .email(request.getEmail())
                .password(
                        passwordEncoder.encode(
                                request.getPassword()
                        )
                )
                .role(Role.USER)
                .build();

        userRepository.save(user);

        return "Registration Successful";
    }

    public String login(LoginRequestDTO request) {

        User user = userRepository.findByEmail(
                request.getEmail()
        ).orElse(null);

        if(user == null) {
            return "Invalid Email";
        }

        boolean matched =
                passwordEncoder.matches(
                        request.getPassword(),
                        user.getPassword()
                );

        if(!matched) {
            return "Invalid Password";
        }

        return "Login Successful";
    }
}