package com.example.news.domain.issue.repository;

import com.example.news.domain.issue.entity.IssueCluster;
import com.example.news.domain.issue.enums.ClusterStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface IssueClusterRepository extends JpaRepository<IssueCluster, Long> {

    Optional<IssueCluster> findBySearchKeywordAndPeriodStartDateAndPeriodEndDate(
            String searchKeyword, LocalDate periodStartDate, LocalDate periodEndDate);

    List<IssueCluster> findByStatus(ClusterStatus status);

    /**
     * 엔티티 조회 없이 status 컬럼만 바로 갱신해서 부분 업데이트를 최적화 한다.
     * 상태 변경은 비즈니스 규칙이 중요하지 않고 단순 상태 갱신이 중요하다고 생각해서 리포지토리쪽에서 UPDATE 구현
     * @param id
     * @param status
     */
    @Modifying(clearAutomatically = true)
    @Transactional
    @Query("UPDATE IssueCluster c SET c.status = :status WHERE c.id = :id")
    void updateStatus(@Param("id") Long id, @Param("status") ClusterStatus status);

    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Transactional
    @Query("UPDATE IssueCluster c SET c.status = :nextStatus WHERE c.id = :id AND c.status = :currentStatus")
    int updateStatusIfCurrent(
            @Param("id") Long id,
            @Param("currentStatus") ClusterStatus currentStatus,
            @Param("nextStatus") ClusterStatus nextStatus
    );
}
