package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.ApiResponse;
import org.ihebut.patent.patent.dto.ValuationCreateRequest;
import org.ihebut.patent.patent.entity.PatentBase;
import org.ihebut.patent.patent.entity.PatentValuationReport;
import org.ihebut.patent.patent.entity.UserPatent;
import org.ihebut.patent.patent.mapper.PatentValuationReportMapper;
import org.ihebut.patent.patent.mapper.UserPatentMapper;
import org.ihebut.patent.patent.service.PatentTableService;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/valuations")
public class ValuationController {
    private final PatentValuationReportMapper reportMapper;
    private final PatentTableService patentTableService;
    private final UserPatentMapper userPatentMapper;

    public ValuationController(PatentValuationReportMapper reportMapper, PatentTableService patentTableService, UserPatentMapper userPatentMapper) {
        this.reportMapper = reportMapper;
        this.patentTableService = patentTableService;
        this.userPatentMapper = userPatentMapper;
    }

    @PostMapping
    public ApiResponse<PatentValuationReport> create(@RequestBody ValuationCreateRequest request) {
        if (request == null || request.getPatentSource() == null || request.getPatentSource().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "patentSource不能为空");
        }
        String source = request.getPatentSource().trim().toUpperCase();
        PatentValuationReport r = new PatentValuationReport();
        r.setPatentSource(source);
        r.setModelVersion(request.getModelVersion());

        String title = "";
        String abs = "";
        if ("EXTERNAL".equals(source)) {
            if (request.getPatentCategory() == null || request.getPatentCategory().isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "patentCategory不能为空");
            }
            if (request.getPatentPublicNum() == null || request.getPatentPublicNum().isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "patentPublicNum不能为空");
            }
            PatentBase p = patentTableService.get(request.getPatentCategory().trim(), request.getPatentPublicNum().trim());
            if (p == null) {
                throw new ResponseStatusException(BAD_REQUEST, "专利不存在");
            }
            r.setPatentCategory(request.getPatentCategory().trim());
            r.setPatentPublicNum(request.getPatentPublicNum().trim());
            title = safe(p.getTitle());
            abs = safe(p.getAbstractText());
        } else if ("USER".equals(source)) {
            if (request.getUserPatentId() == null) {
                throw new ResponseStatusException(BAD_REQUEST, "userPatentId不能为空");
            }
            UserPatent p = userPatentMapper.findById(request.getUserPatentId())
                    .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "用户专利不存在"));
            r.setUserPatentId(p.getId());
            title = safe(p.getTitle());
            abs = safe(p.getAbstractText());
        } else {
            throw new ResponseStatusException(BAD_REQUEST, "patentSource不合法");
        }

        BigDecimal tech = score(title.length(), 20, 120);
        BigDecimal market = score(abs.length(), 50, 600);
        BigDecimal potential = score(keywordHits(title + " " + abs), 1, 8);
        BigDecimal overall = tech.add(market).add(potential).divide(BigDecimal.valueOf(3), 3, RoundingMode.HALF_UP);

        r.setTechValueScore(tech);
        r.setMarketValueScore(market);
        r.setTransformationPotentialScore(potential);
        r.setOverallScore(overall);
        r.setPredictedValue(overall.multiply(BigDecimal.valueOf(1_000_000)).setScale(2, RoundingMode.HALF_UP));
        r.setReportJson("{\"method\":\"heuristic\",\"overall\":" + overall + "}");

        return ApiResponse.ok(reportMapper.save(r));
    }

    @GetMapping
    public ApiResponse<List<PatentValuationReport>> list(
            @RequestParam String patentSource,
            @RequestParam(required = false) String patentCategory,
            @RequestParam(required = false) String patentPublicNum,
            @RequestParam(required = false) Long userPatentId
    ) {
        if (patentSource == null || patentSource.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "patentSource不能为空");
        }
        String source = patentSource.trim().toUpperCase();
        if ("EXTERNAL".equals(source)) {
            if (patentCategory == null || patentCategory.isBlank() || patentPublicNum == null || patentPublicNum.isBlank()) {
                throw new ResponseStatusException(BAD_REQUEST, "patentCategory与patentPublicNum不能为空");
            }
            return ApiResponse.ok(reportMapper.findByPatentSourceAndPatentCategoryAndPatentPublicNum(source, patentCategory.trim(), patentPublicNum.trim()));
        }
        if ("USER".equals(source)) {
            if (userPatentId == null) {
                throw new ResponseStatusException(BAD_REQUEST, "userPatentId不能为空");
            }
            return ApiResponse.ok(reportMapper.findByPatentSourceAndUserPatentId(source, userPatentId));
        }
        throw new ResponseStatusException(BAD_REQUEST, "patentSource不合法");
    }

    private static String safe(String s) {
        return s == null ? "" : s;
    }

    private static BigDecimal score(int value, int min, int max) {
        if (value <= min) return BigDecimal.ZERO.setScale(3, RoundingMode.HALF_UP);
        if (value >= max) return BigDecimal.ONE.setScale(3, RoundingMode.HALF_UP);
        BigDecimal d = BigDecimal.valueOf(value - min).divide(BigDecimal.valueOf(max - min), 3, RoundingMode.HALF_UP);
        return d.max(BigDecimal.ZERO).min(BigDecimal.ONE);
    }

    private static int keywordHits(String text) {
        String t = text == null ? "" : text.toLowerCase();
        int hits = 0;
        for (String k : new String[]{"转化", "许可", "产业", "市场", "降本", "增效", "ai", "算法", "预测", "维护"}) {
            if (t.contains(k)) hits++;
        }
        return hits;
    }
}

