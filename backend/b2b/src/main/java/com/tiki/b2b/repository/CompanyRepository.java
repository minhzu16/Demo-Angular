package com.tiki.b2b.repository;

import com.tiki.b2b.entity.CompanyEntity;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyRepository extends JpaRepository<CompanyEntity, Long> {

    Optional<CompanyEntity> findByTaxId(String taxId);

    Page<CompanyEntity> findByStatus(CompanyEntity.CompanyStatus status, Pageable pageable);
}
