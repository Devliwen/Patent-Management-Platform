package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class ValuationCreateRequest {
    private String patentSource;
    private String patentCategory;
    private String patentPublicNum;
    private Long userPatentId;
    private String modelVersion;
}

