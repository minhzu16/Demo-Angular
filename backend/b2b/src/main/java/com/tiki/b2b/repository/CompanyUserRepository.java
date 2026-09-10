package com.tiki.b2b.repository;

import com.tiki.b2b.entity.CompanyUserEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface CompanyUserRepository extends JpaRepository<CompanyUserEntity, Long> {

    Optional<CompanyUserEntity> findByCompanyIdAndUserIdAndIsActiveTrue(Long companyId, Long userId);

    List<CompanyUserEntity> findByUserIdAndIsActiveTrue(Long userId);

    List<CompanyUserEntity> findByCompanyIdAndIsActiveTrue(Long companyId);
}
