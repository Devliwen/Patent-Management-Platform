package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.UserPatentUpsertRequest;
import org.ihebut.patent.patent.entity.UserPatent;
import org.ihebut.patent.patent.mapper.UserPatentMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/user-patents")
public class UserPatentController {
    private final CurrentUser currentUser;
    private final UserPatentMapper userPatentMapper;

    public UserPatentController(CurrentUser currentUser, UserPatentMapper userPatentMapper) {
        this.currentUser = currentUser;
        this.userPatentMapper = userPatentMapper;
    }

    @PostMapping
    public ApiResponse<UserPatent> create(@RequestBody UserPatentUpsertRequest request) {
        long userId = currentUser.requireUserId();
        if (request == null || request.getCategory() == null || request.getCategory().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "category不能为空");
        }
        if (request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "title不能为空");
        }
        UserPatent p = new UserPatent();
        p.setOwnerUserId(userId);
        apply(p, request);
        return ApiResponse.ok(userPatentMapper.save(p));
    }

    @GetMapping
    public ApiResponse<List<UserPatent>> list(
            @RequestParam(required = false) String owner,
            @RequestParam(required = false) String query,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String visibility
    ) {
        long userId = currentUser.requireUserId();
        String q = query == null ? "" : query.trim();
        String cat = category == null ? null : category.trim();
        String vis = visibility == null ? null : visibility.trim();

        List<UserPatent> base;
        if ("me".equalsIgnoreCase(owner)) {
            if (cat != null && !cat.isBlank()) {
                base = userPatentMapper.findByCategoryAndOwnerUserId(cat, userId);
            } else {
                base = userPatentMapper.findByOwnerUserId(userId);
            }
        } else {
            if (cat != null && !cat.isBlank()) {
                base = userPatentMapper.findByCategoryAndVisibility(cat, "PUBLIC");
            } else {
                base = userPatentMapper.findByVisibility("PUBLIC");
            }
        }

        if (vis != null && !vis.isBlank()) {
            List<UserPatent> filtered = new ArrayList<>();
            for (UserPatent p : base) {
                if (vis.equalsIgnoreCase(p.getVisibility())) filtered.add(p);
            }
            base = filtered;
        }

        if (!q.isEmpty()) {
            List<UserPatent> filtered = new ArrayList<>();
            for (UserPatent p : base) {
                if (contains(p.getTitle(), q)
                        || contains(p.getAbstractText(), q)
                        || contains(p.getApplicant(), q)
                        || contains(p.getInventor(), q)) {
                    filtered.add(p);
                }
            }
            base = filtered;
        }

        return ApiResponse.ok(base);
    }

    @GetMapping("/{id}")
    public ApiResponse<UserPatent> get(@PathVariable Long id) {
        long userId = currentUser.requireUserId();
        UserPatent p = userPatentMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "记录不存在"));
        if (!isVisibleToUser(p, userId)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
        return ApiResponse.ok(p);
    }

    @PutMapping("/{id}")
    public ApiResponse<UserPatent> update(@PathVariable Long id, @RequestBody UserPatentUpsertRequest request) {
        long userId = currentUser.requireUserId();
        UserPatent p = userPatentMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "记录不存在"));
        if (!p.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
        apply(p, request);
        return ApiResponse.ok(userPatentMapper.save(p));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        long userId = currentUser.requireUserId();
        UserPatent p = userPatentMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "记录不存在"));
        if (!p.getOwnerUserId().equals(userId)) {
            throw new ResponseStatusException(FORBIDDEN, "无权限");
        }
        userPatentMapper.deleteById(id);
        return ApiResponse.ok();
    }

    private void apply(UserPatent p, UserPatentUpsertRequest request) {
        if (request == null) return;
        if (request.getCategory() != null && !request.getCategory().isBlank()) p.setCategory(request.getCategory().trim());
        if (request.getPublicNum() != null) p.setPublicNum(request.getPublicNum());
        if (request.getTitle() != null) p.setTitle(request.getTitle());
        if (request.getAbstractText() != null) p.setAbstractText(request.getAbstractText());
        if (request.getIpc() != null) p.setIpc(request.getIpc());
        if (request.getCpc() != null) p.setCpc(request.getCpc());
        if (request.getNec() != null) p.setNec(request.getNec());
        if (request.getApplicant() != null) p.setApplicant(request.getApplicant());
        if (request.getInventor() != null) p.setInventor(request.getInventor());
        if (request.getPatentDetails() != null) p.setPatentDetails(request.getPatentDetails());
        if (request.getVisibility() != null && !request.getVisibility().isBlank()) p.setVisibility(request.getVisibility().trim().toUpperCase());
        if (request.getCooperationCondition() != null) p.setCooperationCondition(request.getCooperationCondition());
    }

    private boolean isVisibleToUser(UserPatent p, long userId) {
        return "PUBLIC".equalsIgnoreCase(p.getVisibility()) || p.getOwnerUserId().equals(userId);
    }

    private boolean contains(String s, String q) {
        return s != null && s.contains(q);
    }
}

