package com.tiki.auth.repository;

import com.tiki.auth.entity.UserAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface UserAddressRepository extends JpaRepository<UserAddress, Long> {
    List<UserAddress> findByUserIdOrderByIsDefaultDescCreatedAtDesc(Long userId);
    void deleteByIdAndUserId(Long id, Long userId);
    List<UserAddress> findByUserIdAndIsDefaultTrue(Long userId);
}
