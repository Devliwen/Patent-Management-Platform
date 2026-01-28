package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.*;
import org.ihebut.patent.patent.entity.ChatMessage;
import org.ihebut.patent.patent.entity.ChatSession;
import org.ihebut.patent.patent.mapper.ChatMessageMapper;
import org.ihebut.patent.patent.mapper.ChatSessionMapper;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.ChatService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@RestController
@RequestMapping("/api/chat")
public class ChatController {
    private final CurrentUser currentUser;
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final ChatService chatService;

    public ChatController(CurrentUser currentUser, ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper, ChatService chatService) {
        this.currentUser = currentUser;
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.chatService = chatService;
    }

    @PostMapping("/sessions")
    public ApiResponse<ChatSessionResponse> createSession(@RequestBody(required = false) ChatCreateSessionRequest request) {
        long userId = currentUser.requireUserId();
        ChatSession session = new ChatSession();
        session.setUserId(userId);
        if (request != null && request.getTitle() != null && !request.getTitle().isBlank()) {
            session.setTitle(request.getTitle().trim());
        }
        session = chatSessionMapper.save(session);
        return ApiResponse.ok(new ChatSessionResponse(session.getId(), session.getTitle(), session.getCreatedAt(), session.getUpdatedAt()));
    }

    @GetMapping("/sessions")
    public ApiResponse<List<ChatSessionResponse>> listSessions() {
        long userId = currentUser.requireUserId();
        List<ChatSessionResponse> sessions = chatSessionMapper.findByUserIdOrderByUpdatedAtDesc(userId).stream()
                .map(s -> new ChatSessionResponse(s.getId(), s.getTitle(), s.getCreatedAt(), s.getUpdatedAt()))
                .toList();
        return ApiResponse.ok(sessions);
    }

    @GetMapping("/sessions/{sessionId}/messages")
    public ApiResponse<List<ChatMessageResponse>> listMessages(@PathVariable("sessionId") Long sessionId) {
        long userId = currentUser.requireUserId();
        if (sessionId == null) {
            throw new ResponseStatusException(BAD_REQUEST, "sessionId不能为空");
        }
        chatSessionMapper.findByIdAndUserId(sessionId, userId)
                .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "会话不存在"));

        List<ChatMessageResponse> messages = chatMessageMapper.findBySessionIdOrderByCreatedAtAsc(sessionId).stream()
                .map(m -> new ChatMessageResponse(m.getId(), m.getRole(), m.getContent(), m.getCreatedAt()))
                .toList();
        return ApiResponse.ok(messages);
    }

    @PostMapping("/ask")
    public ApiResponse<ChatAskResponse> ask(@RequestBody ChatAskRequest request) {
        long userId = currentUser.requireUserId();
        try {
            return ApiResponse.ok(chatService.ask(userId, request));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }
}
