package com.tiki.auth.repository;

import com.tiki.auth.entity.MembershipTransactionEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MembershipTransactionRepository extends JpaRepository<MembershipTransactionEntity, Long> {
    List<MembershipTransactionEntity> findByMembershipIdOrderByCreatedAtDesc(Long membershipId);
}
