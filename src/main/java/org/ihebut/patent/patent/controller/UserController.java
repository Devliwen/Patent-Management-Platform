package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.UserProfileUpdateRequest;
import org.ihebut.patent.patent.entity.*;
import org.ihebut.patent.patent.mapper.*;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

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
        UserAccount user = userAccountMapper.findById(userId).orElse(null);
        UserProfile profile = userProfileMapper.findById(userId).orElse(null);
        ExpertProfile expertProfile = expertProfileMapper.findById(userId).orElse(null);

        Map<String, Object> userView = null;
        if (user != null) {
            userView = new HashMap<>();
            userView.put("id", user.getId());
            userView.put("username", user.getUsername());
            userView.put("phone", user.getPhone());
            userView.put("email", user.getEmail());
            userView.put("status", user.getStatus());
            userView.put("userType", user.getUserType());
            userView.put("createdAt", user.getCreatedAt());
            userView.put("updatedAt", user.getUpdatedAt());
        }

        Map<String, Object> profileView = null;
        if (profile != null) {
            profileView = new HashMap<>();
            profileView.put("userId", profile.getUserId());
            profileView.put("nickname", profile.getNickname());
            profileView.put("avatarUrl", profile.getAvatarUrl());
            profileView.put("realName", profile.getRealName());
            profileView.put("idNumber", profile.getIdNumber());
            profileView.put("createdAt", profile.getCreatedAt());
            profileView.put("updatedAt", profile.getUpdatedAt());
        }

        Map<String, Object> expertProfileView = null;
        if (expertProfile != null) {
            expertProfileView = new HashMap<>();
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

        Map<String, Object> primaryOrg = null;
        UserOrganization rel = userOrganizationMapper.findFirstByUserIdAndIsPrimaryTrue(userId).orElse(null);
        if (rel != null) {
            Organization org = organizationMapper.findById(rel.getOrgId()).orElse(null);
            if (org != null) {
                primaryOrg = new HashMap<>();
                primaryOrg.put("orgId", org.getId());
                primaryOrg.put("name", org.getName());
                primaryOrg.put("type", org.getType());
                primaryOrg.put("relationType", rel.getRelationType());
                primaryOrg.put("positionTitle", rel.getPositionTitle());
            }
        }

        Map<String, Object> data = new HashMap<>();
        data.put("user", userView);
        data.put("profile", profileView);
        data.put("expertProfile", expertProfileView);
        data.put("primaryOrganization", primaryOrg);
        return ApiResponse.ok(data);
    }

    @PutMapping("/me/profile")
    @Transactional
    public ApiResponse<UserProfile> updateProfile(@RequestBody UserProfileUpdateRequest request) {
        long userId = currentUser.requireUserId();
        UserAccount user = userAccountMapper.findById(userId).orElseThrow();
        UserProfile profile = userProfileMapper.findById(userId).orElse(null);
        if (profile == null) {
            profile = new UserProfile();
            profile.setUser(user);
        }
        if (request != null) {
            if (request.getNickname() != null) profile.setNickname(request.getNickname());
            if (request.getAvatarUrl() != null) profile.setAvatarUrl(request.getAvatarUrl());
            if (request.getRealName() != null) profile.setRealName(request.getRealName());
            if (request.getIdNumber() != null) profile.setIdNumber(request.getIdNumber());
        }
        return ApiResponse.ok(userProfileMapper.save(profile));
    }
}
