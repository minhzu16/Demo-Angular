package demo_tt.demo_tt.repository;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

import demo_tt.demo_tt.entity.UserEntity;

public interface UserRepository extends JpaRepository<UserEntity, Long> {
    Optional<UserEntity> findByUsername(String username);
}


