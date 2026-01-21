package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.RequirementPatentMatch;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface RequirementPatentMatchMapper extends JpaRepository<RequirementPatentMatch, Long> {
    List<RequirementPatentMatch> findByRequirementId(Long requirementId);
}

