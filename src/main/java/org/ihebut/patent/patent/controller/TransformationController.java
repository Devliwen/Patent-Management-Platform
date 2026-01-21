package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.entity.TransformationResult;
import org.ihebut.patent.patent.service.TransformationService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

/**
 * 专利转化成果接口。
 *
 * <p>用于记录与查询专利转化的成果信息。</p>
 */
@RestController
@RequestMapping("/api/transformations")
public class TransformationController {
    private final TransformationService transformationService;

    /**
     * 构造器注入。
     *
     * @param transformationService 转化成果服务
     */
    public TransformationController(TransformationService transformationService) {
        this.transformationService = transformationService;
    }

    /**
     * 查询全部转化成果。
     *
     * @return 转化成果列表
     */
    @GetMapping
    public ApiResponse<List<TransformationResult>> getAllResults() {
        return ApiResponse.ok(transformationService.getAllResults());
    }

    /**
     * 创建转化成果记录。
     *
     * @param result 成果信息
     * @return 保存后的成果
     */
    @PostMapping
    public ApiResponse<TransformationResult> createResult(@RequestBody TransformationResult result) {
        return ApiResponse.ok(transformationService.saveResult(result));
    }
}
