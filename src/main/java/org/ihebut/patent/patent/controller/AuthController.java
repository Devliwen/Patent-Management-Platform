package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.LoginRequest;
import org.ihebut.patent.patent.dto.RegisterRequest;
import org.ihebut.patent.patent.entity.UserAccount;
import org.ihebut.patent.patent.entity.UserProfile;
import org.ihebut.patent.patent.mapper.UserAccountMapper;
import org.ihebut.patent.patent.mapper.UserProfileMapper;
import org.ihebut.patent.patent.security.JwtService;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/auth")
public class AuthController {
    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthController(
            UserAccountMapper userAccountMapper,
            UserProfileMapper userProfileMapper,
            PasswordEncoder passwordEncoder,
            JwtService jwtService
    ) {
        this.userAccountMapper = userAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @PostMapping("/register")
    @Transactional
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "username不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "password不能为空");
        }
        if (userAccountMapper.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名已存在");
        }

        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone());
        user.setEmail(request.getEmail());
        user = userAccountMapper.save(user);

        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            UserProfile profile = new UserProfile();
            profile.setUser(user);
            profile.setNickname(request.getNickname().trim());
            userProfileMapper.save(profile);
        }

        return ApiResponse.ok(Map.of("userId", user.getId()));
    }

    @PostMapping("/login")
    public ApiResponse<String> login(@RequestBody LoginRequest request) {
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "username不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "password不能为空");
        }

        UserAccount user = userAccountMapper.findByUsername(request.getUsername().trim())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "用户名或密码错误"));
        if (!passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名或密码错误");
        }
        return ApiResponse.ok(jwtService.createToken(user.getId()));
    }
}
