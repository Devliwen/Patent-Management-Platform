package org.ihebut.patent.patent.service;

import org.ihebut.patent.patent.dto.ChatAskRequest;
import org.ihebut.patent.patent.dto.ChatAskResponse;
import org.ihebut.patent.patent.entity.ChatMessage;
import org.ihebut.patent.patent.entity.ChatSession;
import org.ihebut.patent.patent.mapper.ChatMessageMapper;
import org.ihebut.patent.patent.mapper.ChatSessionMapper;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;
import static org.springframework.http.HttpStatus.NOT_FOUND;

@Service
public class ChatService {
    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final DashScopeChatService dashScopeChatService;

    public ChatService(ChatSessionMapper chatSessionMapper, ChatMessageMapper chatMessageMapper, DashScopeChatService dashScopeChatService) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.dashScopeChatService = dashScopeChatService;
    }

    public ChatAskResponse ask(long userId, ChatAskRequest request) {
        if (request == null || request.getQuestion() == null || request.getQuestion().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "question不能为空");
        }
        int historyLimit = request.getHistoryLimit() == null ? 20 : request.getHistoryLimit();
        if (historyLimit < 0) historyLimit = 0;
        if (historyLimit > 50) historyLimit = 50;

        ChatSession session;
        if (request.getSessionId() == null) {
            session = new ChatSession();
            session.setUserId(userId);
            session.setTitle(buildTitle(request.getQuestion()));
            session = chatSessionMapper.save(session);
        } else {
            session = chatSessionMapper.findByIdAndUserId(request.getSessionId(), userId)
                    .orElseThrow(() -> new ResponseStatusException(NOT_FOUND, "会话不存在"));
            if (session.getTitle() == null || session.getTitle().isBlank()) {
                session.setTitle(buildTitle(request.getQuestion()));
            }
        }

        ChatMessage userMsg = new ChatMessage();
        userMsg.setSessionId(session.getId());
        userMsg.setUserId(userId);
        userMsg.setRole("user");
        userMsg.setContent(request.getQuestion().trim());
        chatMessageMapper.save(userMsg);

        List<ChatMessage> recent = historyLimit == 0
                ? List.of()
                : chatMessageMapper.findTop50BySessionIdOrderByCreatedAtDesc(session.getId());
        List<ChatMessage> history = new ArrayList<>(recent);
        history.sort(Comparator.comparing(ChatMessage::getCreatedAt));

        if (historyLimit > 0 && history.size() > historyLimit) {
            history = history.subList(history.size() - historyLimit, history.size());
        }

        List<DashScopeChatService.Message> messages = new ArrayList<>();
        for (ChatMessage m : history) {
            if (!"user".equals(m.getRole()) && !"assistant".equals(m.getRole()) && !"system".equals(m.getRole())) {
                continue;
            }
            messages.add(new DashScopeChatService.Message(m.getRole(), m.getContent()));
        }

        var aiResp = dashScopeChatService.chatWithMessages(request.getModel(), request.getTemperature(), request.getMaxTokens(), messages);

        ChatMessage aiMsg = new ChatMessage();
        aiMsg.setSessionId(session.getId());
        aiMsg.setUserId(userId);
        aiMsg.setRole("assistant");
        aiMsg.setContent(aiResp.getAnswer() == null ? "" : aiResp.getAnswer());
        chatMessageMapper.save(aiMsg);

        chatSessionMapper.save(session);

        return new ChatAskResponse(session.getId(), aiResp.getAnswer(), aiResp.getModel(), aiResp.getRequestId());
    }

    private static String buildTitle(String question) {
        String s = question == null ? "" : question.trim();
        if (s.length() <= 20) return s;
        return s.substring(0, 20);
    }
}
