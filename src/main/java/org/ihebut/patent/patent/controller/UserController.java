package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.UserProfileUpdateRequest;
import org.ihebut.patent.patent.entity.*;
import org.ihebut.patent.patent.mapper.*;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/users")
public class UserController {
    private final CurrentUser currentUser;
    private final UserAccountMapper userAccountMapper;
    private final UserProfileMapper userProfileMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final UserOrganizationMapper userOrganizationMapper;
    private final OrganizationMapper organizationMapper;

    public UserController(
            CurrentUser currentUser,
            UserAccountMapper userAccountMapper,
            UserProfileMapper userProfileMapper,
            ExpertProfileMapper expertProfileMapper,
            UserOrganizationMapper userOrganizationMapper,
            OrganizationMapper organizationMapper
    ) {
        this.currentUser = currentUser;
        this.userAccountMapper = userAccountMapper;
        this.userProfileMapper = userProfileMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.userOrganizationMapper = userOrganizationMapper;
        this.organizationMapper = organizationMapper;
    }

    @GetMapping("/me")
    public ApiResponse<Map<String, Object>> me() {
        long userId = currentUser.requireUserId();

        // 1. 获取用户基础信息（添加异常处理，避免空指针）
        UserAccount user = userAccountMapper.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "用户不存在"));

        // 2. 构建用户视图（简化代码，避免冗余 null 判断）
        Map<String, Object> userView = new HashMap<>();
        userView.put("id", user.getId());
        userView.put("username", user.getUsername());
        userView.put("phone", user.getPhone());
        userView.put("email", user.getEmail());
        userView.put("status", user.getStatus());
        userView.put("userType", user.getUserType());
        userView.put("createdAt", user.getCreatedAt());
        userView.put("updatedAt", user.getUpdatedAt());

        // 3. 获取用户资料（Optional 简化 null 判断）
        Map<String, Object> profileView = new HashMap<>();
        Optional<UserProfile> profileOpt = userProfileMapper.findById(userId);
        if (profileOpt.isPresent()) {
            UserProfile profile = profileOpt.get();
            profileView.put("userId", profile.getUserId());
            profileView.put("nickname", profile.getNickname());
            profileView.put("avatarUrl", profile.getAvatarUrl());
            profileView.put("realName", profile.getRealName());
            profileView.put("idNumber", profile.getIdNumber());
            profileView.put("createdAt", profile.getCreatedAt());
            profileView.put("updatedAt", profile.getUpdatedAt());
        }

        // 4. 获取专家资料
        Map<String, Object> expertProfileView = new HashMap<>();
        Optional<ExpertProfile> expertProfileOpt = expertProfileMapper.findById(userId);
        if (expertProfileOpt.isPresent()) {
            ExpertProfile expertProfile = expertProfileOpt.get();
            expertProfileView.put("userId", expertProfile.getUserId());
            expertProfileView.put("field", expertProfile.getField());
            expertProfileView.put("expertise", expertProfile.getExpertise());
            expertProfileView.put("achievements", expertProfile.getAchievements());
            expertProfileView.put("contactInfo", expertProfile.getContactInfo());
            expertProfileView.put("certStatus", expertProfile.getCertStatus());
            expertProfileView.put("certSubmitAt", expertProfile.getCertSubmitAt());
            expertProfileView.put("certAuditAt", expertProfile.getCertAuditAt());
            expertProfileView.put("certAuditBy", expertProfile.getCertAuditBy());
            expertProfileView.put("createdAt", expertProfile.getCreatedAt());
            expertProfileView.put("updatedAt", expertProfile.getUpdatedAt());
        }

        // 5. 获取主组织信息
        Map<String, Object> primaryOrg = new HashMap<>();
        Optional<UserOrganization> relOpt = userOrganizationMapper.findFirstByUserIdAndIsPrimaryTrue(userId);
        if (relOpt.isPresent()) {
            UserOrganization rel = relOpt.get();
            Optional<Organization> orgOpt = organizationMapper.findById(rel.getOrgId());
            if (orgOpt.isPresent()) {
                Organization org = orgOpt.get();
                primaryOrg.put("orgId", org.getId());
                primaryOrg.put("name", org.getName());
                primaryOrg.put("type", org.getType());
                primaryOrg.put("relationType", rel.getRelationType());
                primaryOrg.put("positionTitle", rel.getPositionTitle());
            }
        }

        // 6. 组装返回数据
        Map<String, Object> data = new HashMap<>();
        data.put("user", userView);
        data.put("profile", profileView.isEmpty() ? null : profileView);
        data.put("expertProfile", expertProfileView.isEmpty() ? null : expertProfileView);
        data.put("primaryOrganization", primaryOrg.isEmpty() ? null : primaryOrg);

        return ApiResponse.ok(data);
    }

    @PutMapping("/me/profile")
    @Transactional
    public ApiResponse<UserProfile> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        long userId = currentUser.requireUserId();

        // 1. 校验用户存在（抛出自定义异常，避免 500）
        UserAccount user = userAccountMapper.findById(userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "用户不存在"));

        // 2. 获取/创建用户资料（修复分离实体问题）
        UserProfile profile = userProfileMapper.findById(userId).orElse(new UserProfile());

        // 关键修复：避免直接关联 user 实体，改用 userId（或获取持久化状态的 user）
        if (profile.getUserId() == null) {
            profile.setUserId(userId); // 推荐：仅设置 userId，不关联实体
            // 若必须关联实体，替换为：
            // profile.setUser(userAccountMapper.getReferenceById(userId));
        }

        // 3. 更新资料（空值判断，避免覆盖原有数据）
        if (request != null) {
            if (request.getNickname() != null && !request.getNickname().isBlank()) {
                profile.setNickname(request.getNickname().trim());
            }
            if (request.getAvatarUrl() != null) {
                profile.setAvatarUrl(request.getAvatarUrl());
            }
            if (request.getRealName() != null && !request.getRealName().isBlank()) {
                profile.setRealName(request.getRealName().trim());
            }
            if (request.getIdNumber() != null && !request.getIdNumber().isBlank()) {
                profile.setIdNumber(request.getIdNumber().trim());
            }
        }

        // 4. 保存并返回
        UserProfile savedProfile = userProfileMapper.save(profile);
        return ApiResponse.ok(savedProfile);
    }
}