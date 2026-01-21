package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.ExpertProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExpertProfileMapper extends JpaRepository<ExpertProfile, Long> {
    List<ExpertProfile> findByCertStatusAndFieldContainingOrCertStatusAndExpertiseContaining(
            String certStatus1, String field,
            String certStatus2, String expertise
    );
}

