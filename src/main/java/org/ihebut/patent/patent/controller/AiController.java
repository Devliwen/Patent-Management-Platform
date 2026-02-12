package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.AiChatRequest;
import org.ihebut.patent.patent.dto.AiChatResponse;
import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.service.DashScopeChatService;
import org.ihebut.patent.patent.service.PatentEsService;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.web.bind.annotation.*;

import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/ai")
public class AiController {
    private static final Logger log = LoggerFactory.getLogger(AiController.class);
    private final DashScopeChatService dashScopeChatService;
    private final PatentEsService patentEsService;

    public AiController(DashScopeChatService dashScopeChatService, PatentEsService patentEsService) {
        this.dashScopeChatService = dashScopeChatService;
        this.patentEsService = patentEsService;
    }

    @PostMapping("/chat")
    public ApiResponse<AiChatResponse> chat(@RequestBody AiChatRequest request) {
        try {
            return ApiResponse.ok(dashScopeChatService.chat(request));
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    /**
     * 根据需求场景描述进行AI专利检索
     * 1. 调用AI应用分析需求，返回相关的专利公开号列表（空格分隔）
     * 2. 解析专利号，去ES批量检索专利详情
     * 3. 返回AI的分析结果（如果AI有额外解释）和专利详情列表
     */
    @PostMapping("/chat/patent")
    public ApiResponse<Map<String, Object>> chatWithPatent(@RequestBody Map<String, String> request) {
        String requirement = request.get("requirement");
        String sessionId = request.get("sessionId");

        if (requirement == null || requirement.isBlank()) {
            return ApiResponse.fail("requirement不能为空");
        }

        try {
            // 1. 调用 AI 应用 (使用专利专用 App ID)
            // AI 应该配置为：根据用户的输入，分析并返回最相关的专利公开号，多个用空格分隔。
            // 可以在 Prompt 中引导 AI："请分析以下需求，并从你的知识库中找出最匹配的专利，只返回专利公开号，用空格分隔，不要其他废话。"
            AiChatResponse aiResp = dashScopeChatService.chatWithApp(requirement, sessionId, true);
            String aiContent = aiResp.getAnswer();

            // 2. 解析 AI 返回的专利号
            // 假设 AI 返回如: "CN101877498A CN102891517A" 或包含一些解释文本
            // 我们尝试提取所有类似专利号的字符串（简单正则：字母+数字组合，或直接按空格分割）
            // 这里简单按空格分割，并清洗非单词字符
            String[] tokens = aiContent.split("\\s+");
            java.util.List<String> publicNums = new java.util.ArrayList<>();
            for (String t : tokens) {
                // 清洗标点符号，保留字母数字
                String clean = t.replaceAll("[^a-zA-Z0-9]", "");
                if (clean.length() > 5) { // 简单过滤过短的词
                    publicNums.add(clean);
                }
            }

            // 3. 去 ES 批量查询
            java.util.List<org.ihebut.patent.patent.search.PatentSearchDocument> patents = java.util.Collections.emptyList();
            if (!publicNums.isEmpty()) {
                log.info("AI提取专利号，准备查询ES: {}", publicNums);
                patents = patentEsService.findByPublicNums(publicNums);
                log.info("ES查询结果数量: {}", patents.size());
            }

            // 计算未命中的专利号
            java.util.Set<String> foundNums = patents.stream()
                    .map(p -> p.getPublicNum())
                    .collect(Collectors.toSet());
            
            java.util.List<String> notFoundPublicNums = publicNums.stream()
                    .filter(num -> !foundNums.contains(num))
                    .collect(Collectors.toList());

            // 4. 构造返回结果
            Map<String, Object> result = new java.util.HashMap<>();
            result.put("aiAnalysis", aiContent); // AI 的原始回答
            result.put("extractedPublicNums", publicNums); // 提取到的单号
            result.put("patents", patents); // 查到的专利详情
            result.put("notFoundPublicNums", notFoundPublicNums); // 未找到的专利号
            result.put("requestId", aiResp.getRequestId());

            return ApiResponse.ok(result);

        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }
}
