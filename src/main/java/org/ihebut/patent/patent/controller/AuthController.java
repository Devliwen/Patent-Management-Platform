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

import java.time.LocalDateTime;
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

    // 关键：Transactional + 完全不关联实体，仅用 userId
    @Transactional
    @PostMapping("/register")
    public ApiResponse<Map<String, Object>> register(@RequestBody RegisterRequest request) {
        // 1. 严格参数校验
        if (request == null || request.getUsername() == null || request.getUsername().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "username不能为空");
        }
        if (request.getPassword() == null || request.getPassword().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "password不能为空");
        }
        if (userAccountMapper.findByUsername(request.getUsername()).isPresent()) {
            throw new ResponseStatusException(BAD_REQUEST, "用户名已存在");
        }

        // 2. 构建 UserAccount（仅设置基础字段，ID 为 null）
        UserAccount user = new UserAccount();
        user.setUsername(request.getUsername().trim());
        user.setPasswordHash(passwordEncoder.encode(request.getPassword()));
        user.setPhone(request.getPhone() == null ? "" : request.getPhone().trim());
        user.setEmail(request.getEmail() == null ? "" : request.getEmail().trim());
        user.setStatus("1"); // String 类型，匹配实体
        user.setUserType("0"); // String 类型，匹配实体
        user.setCreatedAt(LocalDateTime.now());
        user.setUpdatedAt(LocalDateTime.now());

        // 3. 保存 UserAccount（获取自增 ID）
        user = userAccountMapper.save(user);

        // 4. 保存 UserProfile（核心：只传 userId，给 user 字段赋值，不能为 null）
        if (request.getNickname() != null && !request.getNickname().isBlank()) {
            UserProfile profile = new UserProfile();
             // 核心步骤2：给 user 字段赋值（必须是已保存的 UserAccount 持久化对象）
            profile.setUser(user);
            profile.setNickname(request.getNickname().trim());
            // 注：userId 会通过 @MapsId 自动从 user.getId() 填充，无需手动设置
            // createdAt/updatedAt 由实体的 @PrePersist 自动填充

            // 现在保存 UserProfile 不会报「null one-to-one property」错误
            userProfileMapper.save(profile);
        }

        // 5. 返回成功响应
        return ApiResponse.ok(Map.of("userId", user.getId()));
    }

    @PostMapping("/login")
    public ApiResponse<Map<String, Object>> login(@RequestBody LoginRequest request) {
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

        // 2. 生成JWT令牌（核心！必须返回令牌）
        String token = jwtService.createToken(user.getId(), user.getUsername());

        // 3. 返回令牌 + 用户基本信息
        return ApiResponse.ok(Map.of(
                "token", token,          // 核心：JWT令牌
                "userId", user.getId(),  // 用户ID
                "username", user.getUsername() // 用户名
        ));
    }
}