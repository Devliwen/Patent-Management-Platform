package org.ihebut.patent.patent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Operator;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.elasticsearch._types.query_dsl.TextQueryType;
import org.ihebut.patent.patent.dto.PatentEsSearchResponse;
import org.ihebut.patent.patent.dto.PatentEsStatusResponse;
import org.ihebut.patent.patent.entity.*;
import org.ihebut.patent.patent.mapper.*;
import org.ihebut.patent.patent.search.PatentSearchDocument;
import org.ihebut.patent.patent.search.PatentEsIndexManager;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.mapping.IndexCoordinates;
import org.springframework.data.elasticsearch.core.query.IndexQuery;
import org.springframework.data.elasticsearch.core.query.IndexQueryBuilder;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import static org.springframework.http.HttpStatus.BAD_REQUEST;

@Service
public class PatentEsService {
    private static final Logger log = LoggerFactory.getLogger(PatentEsService.class);
    private final boolean enabled;
    private final String indexName;
    private final ElasticsearchOperations elasticsearchOperations;
    private final ElasticsearchClient elasticsearchClient;
    private final PatentEsIndexManager patentEsIndexManager;

    private final PatentWindMapper patentWindMapper;
    private final PatentSolarMapper patentSolarMapper;
    private final PatentBiomassMapper patentBiomassMapper;
    private final PatentHydrogenMapper patentHydrogenMapper;
    private final PatentLilonMapper patentLilonMapper;

    public PatentEsService(
            @Value("${search.es.enabled:false}") boolean enabled,
            @Value("${search.es.index.patent:patents}") String indexName,
            ElasticsearchOperations elasticsearchOperations,
            ElasticsearchClient elasticsearchClient,
            PatentEsIndexManager patentEsIndexManager,
            PatentWindMapper patentWindMapper,
            PatentSolarMapper patentSolarMapper,
            PatentBiomassMapper patentBiomassMapper,
            PatentHydrogenMapper patentHydrogenMapper,
            PatentLilonMapper patentLilonMapper
    ) {
        this.enabled = enabled;
        this.indexName = indexName;
        this.elasticsearchOperations = elasticsearchOperations;
        this.elasticsearchClient = elasticsearchClient;
        this.patentEsIndexManager = patentEsIndexManager;
        this.patentWindMapper = patentWindMapper;
        this.patentSolarMapper = patentSolarMapper;
        this.patentBiomassMapper = patentBiomassMapper;
        this.patentHydrogenMapper = patentHydrogenMapper;
        this.patentLilonMapper = patentLilonMapper;
    }

    public void reindexAll() {
        requireEnabled();
        long start = System.currentTimeMillis();
        log.info("ES重建索引开始 index={}", indexName);
        patentEsIndexManager.recreateIndex();
        reindexCategory("wind");
        reindexCategory("solar");
        reindexCategory("biomass");
        reindexCategory("hydrogen");
        reindexCategory("lilon");
        log.info("ES重建索引完成 index={} costMs={}", indexName, System.currentTimeMillis() - start);
    }

    public void reindexCategory(String category) {
        requireEnabled();
        patentEsIndexManager.ensureIndexExists();
        String cat = normalizeCategory(category);
        int page = 0;
        int size = 500;
        long start = System.currentTimeMillis();
        long total = -1;
        long processed = 0;
        log.info("ES索引同步开始 index={} category={} pageSize={}", indexName, cat, size);
        for (;;) {
            Page<? extends PatentBase> p = fetchPage(cat, PageRequest.of(page, size));
            if (p.isEmpty()) break;
            if (total < 0) {
                total = p.getTotalElements();
            }

            List<IndexQuery> queries = new ArrayList<>(p.getNumberOfElements());
            for (PatentBase patent : p.getContent()) {
                PatentSearchDocument doc = toDoc(cat, patent);
                IndexQuery iq = new IndexQueryBuilder()
                        .withId(doc.getId())
                        .withObject(doc)
                        .build();
                queries.add(iq);
            }
            elasticsearchOperations.bulkIndex(queries, IndexCoordinates.of(indexName));
            processed += p.getNumberOfElements();
            if (total > 0) {
                log.info("ES索引同步进度 index={} category={} page={}/{} batch={} processed={}/{}",
                        indexName, cat, p.getNumber() + 1, p.getTotalPages(), p.getNumberOfElements(), processed, total);
            } else {
                log.info("ES索引同步进度 index={} category={} page={} batch={} processed={}",
                        indexName, cat, p.getNumber() + 1, p.getNumberOfElements(), processed);
            }
            page++;
            if (p.isLast()) break;
        }
        elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).refresh();
        log.info("ES索引同步完成 index={} category={} total={} costMs={}",
                indexName, cat, Math.max(total, processed), System.currentTimeMillis() - start);
    }

    public void indexOne(String category, String publicNum) {
        requireEnabled();
        patentEsIndexManager.ensureIndexExists();
        if (publicNum == null || publicNum.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "publicNum不能为空");
        }
        String cat = normalizeCategory(category);
        PatentBase patent = fetchOne(cat, publicNum.trim())
                .orElseThrow(() -> new ResponseStatusException(BAD_REQUEST, "未找到该专利：" + publicNum));

        PatentSearchDocument doc = toDoc(cat, patent);
        IndexQuery iq = new IndexQueryBuilder()
                .withId(doc.getId())
                .withObject(doc)
                .build();
        elasticsearchOperations.bulkIndex(List.of(iq), IndexCoordinates.of(indexName));
        elasticsearchOperations.indexOps(IndexCoordinates.of(indexName)).refresh();
    }

    /**
     * 根据公开号精确查找专利
     */
    public Optional<PatentSearchDocument> findByPublicNum(String publicNum) {
        requireEnabled();
        if (publicNum == null || publicNum.isBlank()) {
            return Optional.empty();
        }
        try {
            var resp = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(q -> q.term(t -> t.field("public_num").value(publicNum.trim())))
                            .size(1),
                    Map.class
            );
            if (resp.hits().hits().isEmpty()) {
                return Optional.empty();
            }
            Object srcObj = resp.hits().hits().get(0).source();
            if (!(srcObj instanceof Map<?, ?> src)) return Optional.empty();
            return Optional.of(mapToDoc(src));
        } catch (Exception e) {
            log.error("ES查询失败 publicNum={}", publicNum, e);
            return Optional.empty();
        }
    }

    /**
     * 根据公开号列表批量查找专利
     */
    public List<PatentSearchDocument> findByPublicNums(List<String> publicNums) {
        requireEnabled();
        // 1. 参数校验与预处理：去空、去重
        if (publicNums == null || publicNums.isEmpty()) {
            return Collections.emptyList();
        }

        List<String> validNums = publicNums.stream()
                .filter(s -> s != null && !s.isBlank())
                .map(String::trim)
                .distinct()
                .toList();

        if (validNums.isEmpty()) {
            return Collections.emptyList();
        }

        try {
            Query q = Query.of(query -> query.bool(b -> {
                for (String num : validNums) {
                    b.should(s -> s.term(t -> t.field("public_num").value(num)));
                }
                b.minimumShouldMatch("1");
                return b;
            }));

            var resp = elasticsearchClient.search(s -> s
                            .index(indexName)
                            .query(q)
                            .size(validNums.size()),
                    Map.class
            );

            // 3. 结果处理：提取 Source 并过滤空值
            return resp.hits().hits().stream()
                    .map(h -> h.source())
                    .filter(java.util.Objects::nonNull)
                    .filter(src -> src instanceof Map<?, ?>)
                    .map(src -> mapToDoc((Map<?, ?>) src))
                    .toList();
        } catch (Exception e) {
            log.error("ES批量查询失败 index={} publicNums={}", indexName, validNums, e);
            throw new ResponseStatusException(BAD_REQUEST, "ES批量查询失败：" + e.getMessage());
        }
    }

    public PatentEsStatusResponse status() {
        requireEnabled();
        try {
            boolean exists = elasticsearchClient.indices().exists(e -> e.index(indexName)).value();
            long count = 0;
            if (exists) {
                count = elasticsearchClient.count(c -> c.index(indexName)).count();
            }
            return new PatentEsStatusResponse(indexName, exists, count);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "ES状态查询失败：" + e.getMessage());
        }
    }

    public PatentEsSearchResponse search(String category, String query, int page, int size, boolean phrase) {
        requireEnabled();
        patentEsIndexManager.ensureIndexExists();
        if (query == null || query.isBlank()) {
            throw new ResponseStatusException(BAD_REQUEST, "query不能为空");
        }
        if (page < 0) page = 0;
        if (size <= 0) size = 10;
        if (size > 50) size = 50;

        int finalPage = page;
        int finalSize = size;
        Query q = buildQuery(category, query.trim(), phrase);

        try {
            var resp = elasticsearchClient.search(s -> {
                        s.index(indexName);
                        s.from(finalPage * finalSize);
                        s.size(finalSize);
                        s.query(q);
                        s.highlight(h -> h
                                .preTags("<em>")
                                .postTags("</em>")
                                .fields("title", f -> f)
                                .fields("abstract", f -> f)
                                .fields("patent_details", f -> f)
                        );
                        return s;
                    },
                    Map.class
            );

            List<PatentEsSearchResponse.Hit> out = new ArrayList<>();
            for (var h : resp.hits().hits()) {
                Object srcObj = h.source();
                if (!(srcObj instanceof Map<?, ?> src)) continue;
                Map<String, List<String>> hl = h.highlight() == null ? Collections.emptyMap() : h.highlight();
                out.add(new PatentEsSearchResponse.Hit(
                        getString(src, "category"),
                        firstNonBlank(getString(src, "public_num"), getString(src, "publicNum")),
                        getString(src, "title"),
                        firstNonBlank(getString(src, "abstract"), getString(src, "abstractText")),
                        getString(src, "applicant"),
                        getString(src, "inventor"),
                        h.score(),
                        joinHighlight(hl.get("title")),
                        joinHighlight(hl.get("abstract")),
                        joinHighlight(hl.get("patent_details"))
                ));
            }

            long total = resp.hits().total() == null ? out.size() : resp.hits().total().value();
            return new PatentEsSearchResponse(total, out);
        } catch (Exception e) {
            throw new ResponseStatusException(BAD_REQUEST, "ES查询失败：" + e.getMessage());
        }
    }

    private Query buildQuery(String category, String query, boolean phrase) {
        List<String> fields = List.of(
                "public_num^6",
                "title^4",
                "abstract^2",
                "applicant^1",
                "inventor^1",
                "patent_details^1",
                "ipc^1",
                "cpc^1",
                "nec^1",
                "all_text"
        );

        Query base = Query.of(q -> q.multiMatch(m -> {
            m.query(query);
            m.fields(fields);
            m.operator(Operator.And);
            if (phrase) {
                m.type(TextQueryType.Phrase);
                m.slop(2);
            }
            return m;
        }));
        if (category == null || category.isBlank()) {
            return base;
        }
        String cat = normalizeCategory(category);
        return Query.of(q -> q.bool(b -> b
                .must(base)
                .filter(f -> f.term(t -> t.field("category").value(cat)))
        ));
    }

    private Page<? extends PatentBase> fetchPage(String category, Pageable pageable) {
        return switch (category) {
            case "wind" -> patentWindMapper.findAll(pageable);
            case "solar" -> patentSolarMapper.findAll(pageable);
            case "biomass" -> patentBiomassMapper.findAll(pageable);
            case "hydrogen" -> patentHydrogenMapper.findAll(pageable);
            case "lilon" -> patentLilonMapper.findAll(pageable);
            default -> throw new ResponseStatusException(BAD_REQUEST, "不支持的category：" + category);
        };
    }

    private Optional<? extends PatentBase> fetchOne(String category, String publicNum) {
        return switch (category) {
            case "wind" -> patentWindMapper.findById(publicNum);
            case "solar" -> patentSolarMapper.findById(publicNum);
            case "biomass" -> patentBiomassMapper.findById(publicNum);
            case "hydrogen" -> patentHydrogenMapper.findById(publicNum);
            case "lilon" -> patentLilonMapper.findById(publicNum);
            default -> throw new ResponseStatusException(BAD_REQUEST, "不支持的category：" + category);
        };
    }

    private static PatentSearchDocument toDoc(String category, PatentBase p) {
        PatentSearchDocument d = new PatentSearchDocument();
        d.setCategory(category);
        d.setPublicNum(p.getPublicNum());
        d.setId(category + ":" + p.getPublicNum());
        d.setTitle(p.getTitle());
        d.setAbstractText(p.getAbstractText());
        d.setApplicant(p.getApplicant());
        d.setInventor(p.getInventor());
        d.setIpc(p.getIpc());
        d.setCpc(p.getCpc());
        d.setNec(p.getNec());
        d.setPatentDetails(p.getPatentDetails());
        return d;
    }

    private String normalizeCategory(String category) {
        if (category == null) throw new ResponseStatusException(BAD_REQUEST, "category不能为空");
        String c = category.trim().toLowerCase(Locale.ROOT);
        return switch (c) {
            case "wind", "solar", "biomass", "hydrogen", "lilon" -> c;
            default -> throw new ResponseStatusException(BAD_REQUEST, "不支持的category：" + category);
        };
    }

    private void requireEnabled() {
        if (!enabled) {
            throw new ResponseStatusException(BAD_REQUEST, "Elasticsearch未启用，请设置 ES_ENABLED=true 并配置 ES_URIS");
        }
    }

    private static String joinHighlight(List<String> fragments) {
        if (fragments == null || fragments.isEmpty()) return null;
        return String.join(" ... ", fragments);
    }

    private static String getString(Map<?, ?> src, String key) {
        Object v = src.get(key);
        if (v == null) return null;
        return String.valueOf(v);
    }

    private static String firstNonBlank(String a, String b) {
        if (a != null && !a.isBlank()) return a;
        if (b != null && !b.isBlank()) return b;
        return null;
    }

    private static PatentSearchDocument mapToDoc(Map<?, ?> src) {
        PatentSearchDocument d = new PatentSearchDocument();
        d.setCategory(getString(src, "category"));
        String publicNum = firstNonBlank(getString(src, "public_num"), getString(src, "publicNum"));
        d.setPublicNum(publicNum);
        String id = firstNonBlank(getString(src, "id"), getString(src, "_id"));
        if (id == null || id.isBlank()) {
            if (d.getCategory() != null && publicNum != null) {
                id = d.getCategory() + ":" + publicNum;
            }
        }
        d.setId(id);
        d.setTitle(getString(src, "title"));
        d.setAbstractText(firstNonBlank(getString(src, "abstract"), getString(src, "abstractText")));
        d.setApplicant(getString(src, "applicant"));
        d.setInventor(getString(src, "inventor"));
        d.setIpc(getString(src, "ipc"));
        d.setCpc(getString(src, "cpc"));
        d.setNec(getString(src, "nec"));
        d.setPatentDetails(firstNonBlank(getString(src, "patent_details"), getString(src, "patentDetails")));
        return d;
    }
}
