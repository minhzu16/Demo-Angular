package com.tiki.auth.repository;

import com.tiki.auth.entity.MembershipPlanEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MembershipPlanRepository extends JpaRepository<MembershipPlanEntity, Long> {

    Optional<MembershipPlanEntity> findByCode(String code);

    List<MembershipPlanEntity> findByIsActiveTrueOrderByDisplayOrderAsc();
}
