package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.PatentEsSearchResponse;
import org.ihebut.patent.patent.dto.PatentEsStatusResponse;
import org.ihebut.patent.patent.service.PatentEsService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/patents/es")
public class PatentEsController {
    private final PatentEsService patentEsService;

    public PatentEsController(PatentEsService patentEsService) {
        this.patentEsService = patentEsService;
    }

    @GetMapping("/search")
    public ApiResponse<PatentEsSearchResponse> search(
            @RequestParam(required = false) String category,
            @RequestParam String query,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "false") boolean phrase
    ) {
        try {
            return ApiResponse.ok(patentEsService.search(category, query, page, size, phrase));
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    @GetMapping("/status")
    public ApiResponse<PatentEsStatusResponse> status() {
        try {
            return ApiResponse.ok(patentEsService.status());
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    @PostMapping("/reindex")
    public ApiResponse<Void> reindex(@RequestParam(required = false) String category) {
        try {
            if (category == null || category.isBlank()) {
                patentEsService.reindexAll();
            } else {
                patentEsService.reindexCategory(category);
            }
            return ApiResponse.ok();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }

    @PostMapping("/index-one")
    public ApiResponse<Void> indexOne(@RequestParam String category, @RequestParam String publicNum) {
        try {
            patentEsService.indexOne(category, publicNum);
            return ApiResponse.ok();
        } catch (ResponseStatusException e) {
            throw e;
        } catch (Exception e) {
            return ApiResponse.fail(e.getMessage() == null ? "请求失败" : e.getMessage());
        }
    }
}
