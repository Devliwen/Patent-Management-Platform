package org.ihebut.patent.patent.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.util.List;

@Data
@AllArgsConstructor
public class PatentEsSearchResponse {
    private long total;
    private List<Hit> hits;

    @Data
    @AllArgsConstructor
    public static class Hit {
        private String category;
        private String publicNum;
        private String title;
        private String abstractText;
        private String applicant;
        private String inventor;
        private Double score;
        private String highlightTitle;
        private String highlightAbstractText;
        private String highlightPatentDetails;
    }
}
