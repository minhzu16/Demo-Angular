package com.tiki.auth.repository;

import com.tiki.auth.entity.MembershipEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipRepository extends JpaRepository<MembershipEntity, Long> {

    Optional<MembershipEntity> findByUserId(Long userId);

    @Query("SELECT m FROM MembershipEntity m WHERE m.userId = :userId " +
           "AND m.status IN ('ACTIVE', 'TRIAL') " +
           "AND m.endDate >= :now")
    Optional<MembershipEntity> findActiveMembership(
            @Param("userId") Long userId,
            @Param("now") LocalDateTime now);

    List<MembershipEntity> findByStatusAndAutoRenewTrueAndNextBillingDateBefore(
            MembershipEntity.MembershipStatus status,
            LocalDateTime date);
}
