package org.ihebut.patent.patent.mapper;

import org.ihebut.patent.patent.entity.Requirement;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * 需求数据访问层（Mapper）。
 *
 * <p>当前采用 Spring Data JPA 的接口方式实现。</p>
 */
public interface RequirementMapper extends JpaRepository<Requirement, Long> {
    /**
     * 按状态查询需求。
     *
     * @param status 状态
     * @return 需求列表
     */
    List<Requirement> findByStatus(String status);
}
