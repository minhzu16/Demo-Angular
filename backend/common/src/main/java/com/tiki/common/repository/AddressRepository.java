package com.tiki.common.repository;

import com.tiki.common.entity.AddressEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface AddressRepository extends JpaRepository<AddressEntity, Integer> {
    List<AddressEntity> findByUserId(Long userId);
}
