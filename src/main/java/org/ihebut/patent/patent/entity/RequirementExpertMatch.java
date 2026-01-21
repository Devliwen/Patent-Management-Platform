package org.ihebut.patent.patent.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "requirement_expert_match")
@Data
public class RequirementExpertMatch {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "requirement_id", nullable = false)
    private Long requirementId;

    @Column(name = "expert_user_id", nullable = false)
    private Long expertUserId;

    @Column(name = "match_score", precision = 6, scale = 3)
    private BigDecimal matchScore;

    @Column(name = "match_reason", columnDefinition = "text")
    private String matchReason;

    @Column(name = "created_at", nullable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void prePersist() {
        if (this.createdAt == null) {
            this.createdAt = LocalDateTime.now();
        }
    }
}
