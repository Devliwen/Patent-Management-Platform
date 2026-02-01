package org.ihebut.patent.patent.search;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;

@Data
@Document(indexName = "#{@patentIndexName}")
public class PatentSearchDocument {
    @Id
    private String id;

    @Field(type = FieldType.Keyword)
    private String category;

    @Field(type = FieldType.Keyword, name = "public_num")
    @JsonProperty("public_num")
    private String publicNum;

    @Field(type = FieldType.Text)
    private String title;

    @Field(type = FieldType.Text, name = "abstract")
    @JsonProperty("abstract")
    private String abstractText;

    @Field(type = FieldType.Text)
    private String applicant;

    @Field(type = FieldType.Text)
    private String inventor;

    @Field(type = FieldType.Text)
    private String ipc;

    @Field(type = FieldType.Text)
    private String cpc;

    @Field(type = FieldType.Text)
    private String nec;

    @Field(type = FieldType.Text, name = "patent_details")
    @JsonProperty("patent_details")
    private String patentDetails;
}
