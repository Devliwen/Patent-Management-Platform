package org.ihebut.patent.patent.controller;

import org.ihebut.patent.patent.dto.*;
import org.ihebut.patent.patent.entity.*;
import org.ihebut.patent.patent.mapper.*;
import org.ihebut.patent.patent.security.CurrentUser;
import org.ihebut.patent.patent.service.PatentTableService;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.ArrayList;
import java.util.List;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@RestController
@RequestMapping("/api/requirements")
public class RequirementController {
    private final CurrentUser currentUser;
    private final RequirementMapper requirementMapper;
    private final PatentTableService patentTableService;
    private final UserPatentMapper userPatentMapper;
    private final ExpertProfileMapper expertProfileMapper;
    private final RequirementPatentMatchMapper requirementPatentMatchMapper;
    private final RequirementExpertMatchMapper requirementExpertMatchMapper;

    public RequirementController(
            CurrentUser currentUser,
            RequirementMapper requirementMapper,
            PatentTableService patentTableService,
            UserPatentMapper userPatentMapper,
            ExpertProfileMapper expertProfileMapper,
            RequirementPatentMatchMapper requirementPatentMatchMapper,
            RequirementExpertMatchMapper requirementExpertMatchMapper
    ) {
        this.currentUser = currentUser;
        this.requirementMapper = requirementMapper;
        this.patentTableService = patentTableService;
        this.userPatentMapper = userPatentMapper;
        this.expertProfileMapper = expertProfileMapper;
        this.requirementPatentMatchMapper = requirementPatentMatchMapper;
        this.requirementExpertMatchMapper = requirementExpertMatchMapper;
    }

    @PostMapping
    public ApiResponse<Requirement> create(@RequestBody RequirementCreateRequest request) {
        long userId = currentUser.requireUserId();
        if (request == null || request.getTitle() == null || request.getTitle().isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "title不能为空");
        }
        Requirement r = new Requirement();
        r.setTitle(request.getTitle().trim());
        r.setDescription(request.getDescription());
        r.setKeywords(request.getKeywords());
        r.setTechDirection(request.getTechDirection());
        r.setCooperationMode(request.getCooperationMode());
        r.setRequesterUserId(userId);
        r.setRequesterOrgId(request.getRequesterOrgId());
        return ApiResponse.ok(requirementMapper.save(r));
    }

    @GetMapping("/{id}/match-patents")
    public ApiResponse<List<PatentMatchResult>> matchPatents(@PathVariable Long id) {
        long userId = currentUser.requireUserId();
        Requirement req = requirementMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "需求不存在"));
        String keywords = (req.getKeywords() != null && !req.getKeywords().isBlank()) ? req.getKeywords() : req.getTitle();
        String query = keywords == null ? "" : keywords.trim();

        List<PatentMatchResult> results = new ArrayList<>();

        if (!query.isEmpty()) {
            results.addAll(toExternal("wind", patentTableService.search("wind", query)));
            results.addAll(toExternal("solar", patentTableService.search("solar", query)));
            results.addAll(toExternal("biomass", patentTableService.search("biomass", query)));
            results.addAll(toExternal("hydrogen", patentTableService.search("hydrogen", query)));
            results.addAll(toExternal("lilon", patentTableService.search("lilon", query)));

            for (UserPatent p : userPatentMapper.findByVisibility("PUBLIC")) {
                if (contains(p.getTitle(), query)
                        || contains(p.getAbstractText(), query)
                        || contains(p.getApplicant(), query)
                        || contains(p.getInventor(), query)) {
                    results.add(new PatentMatchResult("USER", p.getCategory(), p.getPublicNum(), p.getId(), p.getTitle(), p.getApplicant(), p.getInventor()));
                }
            }

            for (UserPatent p : userPatentMapper.findByOwnerUserId(userId)) {
                if (!"PUBLIC".equalsIgnoreCase(p.getVisibility())
                        && (contains(p.getTitle(), query)
                        || contains(p.getAbstractText(), query)
                        || contains(p.getApplicant(), query)
                        || contains(p.getInventor(), query))) {
                    results.add(new PatentMatchResult("USER", p.getCategory(), p.getPublicNum(), p.getId(), p.getTitle(), p.getApplicant(), p.getInventor()));
                }
            }
        }

        return ApiResponse.ok(results);
    }

    @GetMapping("/{id}/match-experts")
    public ApiResponse<List<ExpertMatchResult>> matchExperts(@PathVariable Long id) {
        Requirement req = requirementMapper.findById(id).orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "需求不存在"));
        String keywords = (req.getKeywords() != null && !req.getKeywords().isBlank()) ? req.getKeywords() : req.getTitle();
        String q = keywords == null ? "" : keywords.trim();

        List<ExpertMatchResult> result = new ArrayList<>();
        if (q.isEmpty()) return ApiResponse.ok(result);

        List<ExpertProfile> matches = expertProfileMapper
                .findByCertStatusAndFieldContainingOrCertStatusAndExpertiseContaining("APPROVED", q, "APPROVED", q);
        for (ExpertProfile p : matches) {
            result.add(new ExpertMatchResult(p.getUserId(), p.getField(), p.getExpertise(), p.getContactInfo()));
        }
        return ApiResponse.ok(result);
    }

    @PostMapping("/{id}/match-patents/persist")
    @Transactional
    public ApiResponse<Void> persistPatentMatches(@PathVariable Long id, @RequestBody RequirementPatentMatchPersistRequest request) {
        if (!requirementMapper.existsById(id)) {
            throw new ResponseStatusException(BAD_REQUEST, "需求不存在");
        }
        if (request != null && request.getItems() != null) {
            for (RequirementPatentMatchPersistRequest.Item item : request.getItems()) {
                RequirementPatentMatch m = new RequirementPatentMatch();
                m.setRequirementId(id);
                m.setPatentSource("EXTERNAL");
                m.setPatentCategory(item.getPatentCategory());
                m.setPatentPublicNum(item.getPatentPublicNum());
                m.setMatchScore(toScore(item.getMatchScore()));
                m.setMatchReason(item.getMatchReason());
                requirementPatentMatchMapper.save(m);
            }
        }
        return ApiResponse.ok();
    }

    @PostMapping("/{id}/match-experts/persist")
    @Transactional
    public ApiResponse<Void> persistExpertMatches(@PathVariable Long id, @RequestBody RequirementExpertMatchPersistRequest request) {
        if (!requirementMapper.existsById(id)) {
            throw new ResponseStatusException(BAD_REQUEST, "需求不存在");
        }
        if (request != null && request.getItems() != null) {
            for (RequirementExpertMatchPersistRequest.Item item : request.getItems()) {
                RequirementExpertMatch m = new RequirementExpertMatch();
                m.setRequirementId(id);
                m.setExpertUserId(item.getExpertId());
                m.setMatchScore(toScore(item.getMatchScore()));
                m.setMatchReason(item.getMatchReason());
                requirementExpertMatchMapper.save(m);
            }
        }
        return ApiResponse.ok();
    }

    private List<PatentMatchResult> toExternal(String category, List<?> patents) {
        List<PatentMatchResult> items = new ArrayList<>();
        for (Object p : patents) {
            if (p instanceof PatentBase patent) {
                items.add(new PatentMatchResult(
                        "EXTERNAL",
                        category,
                        patent.getPublicNum(),
                        null,
                        patent.getTitle(),
                        patent.getApplicant(),
                        patent.getInventor()
                ));
            }
        }
        return items;
    }

    private boolean contains(String s, String q) {
        return s != null && s.contains(q);
    }

    private static BigDecimal toScore(Double v) {
        if (v == null) return null;
        return BigDecimal.valueOf(v).setScale(3, RoundingMode.HALF_UP);
    }
}
