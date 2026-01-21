package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.entity.AuditLog;
import org.ihebut.patent.patent.mapper.AuditLogMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/audit-logs")
public class AuditLogController {
    private final CurrentUser currentUser;
    private final AuditLogMapper auditLogMapper;

    public AuditLogController(CurrentUser currentUser, AuditLogMapper auditLogMapper) {
        this.currentUser = currentUser;
        this.auditLogMapper = auditLogMapper;
    }

    @GetMapping
    public ApiResponse<List<AuditLog>> list(
            @RequestParam(required = false) Long userId,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        currentUser.requireUserId();

        LocalDateTime fromDt = parseTime(from);
        LocalDateTime toDt = parseTime(to);

        List<AuditLog> base;
        if (fromDt != null && toDt != null) {
            base = auditLogMapper.findByCreatedAtBetween(fromDt, toDt);
        } else if (userId != null) {
            base = auditLogMapper.findByUserId(userId);
        } else {
            base = auditLogMapper.findAll();
        }

        List<AuditLog> filtered = new ArrayList<>();
        for (AuditLog a : base) {
            if (userId != null && (a.getUserId() == null || !a.getUserId().equals(userId))) continue;
            if (action != null && !action.isBlank() && (a.getAction() == null || !a.getAction().contains(action.trim()))) continue;
            filtered.add(a);
        }
        return ApiResponse.ok(filtered);
    }

    private LocalDateTime parseTime(String s) {
        if (s == null || s.isBlank()) return null;
        try {
            return LocalDateTime.parse(s.trim());
        } catch (DateTimeParseException e) {
            throw new ResponseStatusException(BAD_REQUEST, "时间格式不合法");
        }
    }
}

