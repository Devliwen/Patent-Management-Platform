package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.AiChatRequest;
import org.ihebut.patent.patent.dto.AiChatResponse;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.service.DashScopeChatService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private final DashScopeChatService dashScopeChatService;

    public AiController(DashScopeChatService dashScopeChatService) {
        this.dashScopeChatService = dashScopeChatService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        try {
            return ApiResponse.ok(dashScopeChatService.chat(request));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }
}
