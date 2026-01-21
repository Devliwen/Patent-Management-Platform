package org.ihebut.patent.patent.dto;

import lombok.Data;

@Data
public class RequirementCreateRequest {
    private String title;
    private String description;
    private String keywords;
    private String techDirection;
    private String cooperationMode;
    private Long requesterOrgId;
}

