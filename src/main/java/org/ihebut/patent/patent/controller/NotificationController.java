package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.NotificationCreateRequest;
import org.ihebut.patent.patent.entity.UserNotification;
import org.ihebut.patent.patent.mapper.UserNotificationMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/notifications")
public class NotificationController {
    private final CurrentUser currentUser;
    private final UserNotificationMapper userNotificationMapper;

    public NotificationController(CurrentUser currentUser, UserNotificationMapper userNotificationMapper) {
        this.currentUser = currentUser;
        this.userNotificationMapper = userNotificationMapper;
    }

    @GetMapping
    public ApiResponse<List<UserNotification>> list(@RequestParam(required = false) Boolean read) {
        long userId = currentUser.requireUserId();
        if (read == null) {
            return ApiResponse.ok(userNotificationMapper.findByUserId(userId));
        }
        return ApiResponse.ok(userNotificationMapper.findByUserIdAndReadFlag(userId, read));
    }

    @PostMapping
    public ApiResponse<UserNotification> create(@RequestBody NotificationCreateRequest request) {
        if (request == null || request.getUserId() == null) {
            throw new ResponseStatusException(BAD_REQUEST, "userId不能为空");
        }
        if (request.getType() == null || request.getType().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "type不能为空");
        }
        UserNotification n = new UserNotification();
        n.setUserId(request.getUserId());
        n.setType(request.getType().trim());
        n.setTitle(request.getTitle());
        n.setContent(request.getContent());
        n.setRelatedRequirementId(request.getRelatedRequirementId());
        return ApiResponse.ok(userNotificationMapper.save(n));
    }

    @PutMapping("/{id}/read")
    public ApiResponse<UserNotification> markRead(@PathVariable Long id) {
        long userId = currentUser.requireUserId();
        UserNotification n = userNotificationMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "通知不存在"));
        if (!n.getUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
        n.setReadFlag(true);
        return ApiResponse.ok(userNotificationMapper.save(n));
    }
}

