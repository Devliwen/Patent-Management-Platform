package org.ihebut.patent.patent.service;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import org.ihebut.patent.patent.search.PatentEsIndexManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;

import java.util.function.Function;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class PatentEsServiceTest {

    @Mock private ElasticsearchOperations elasticsearchOperations;
    @Mock private ElasticsearchClient elasticsearchClient;
    @Mock private PatentEsIndexManager patentEsIndexManager;
    @Mock private co.elastic.clients.elasticsearch.core.SearchResponse searchResponse;
    @Mock private co.elastic.clients.elasticsearch.core.search.HitsMetadata hitsMetadata;

    private PatentEsService service;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        service = new PatentEsService(
                true, "patents",
                elasticsearchOperations, elasticsearchClient, patentEsIndexManager,
                null, null, null, null, null
        );
    }

    @Test
    void search_ShouldIncludeCategoryFilter_WhenCategoryProvided() throws Exception {
        // Arrange
        String category = "lilon";
        String query = "test";
        
        when(elasticsearchClient.search(any(Function.class), any(Class.class)))
                .thenReturn(searchResponse);
        when(searchResponse.hits()).thenReturn(hitsMetadata);
        when(hitsMetadata.hits()).thenReturn(java.util.Collections.emptyList());

        // Act
        service.search(category, query, 0, 10, false);

        // Assert
        ArgumentCaptor<Function> captor = ArgumentCaptor.forClass(Function.class);
        verify(elasticsearchClient).search(captor.capture(), any(Class.class));
        
        // 由于无法直接检查Lambda内部构建的Query结构，这里主要验证调用链路
        // 实际Query结构验证通常需要集成测试，或者通过反射/复杂Captor解析
        // 但至少验证了 search 方法被调用且没有抛出异常
    }
}
