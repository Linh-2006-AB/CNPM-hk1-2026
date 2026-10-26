package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.OwnerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.Optional;

@Repository
public interface OwnerProfileRepository extends JpaRepository<OwnerProfile, Integer> {
    Optional<OwnerProfile> findByUserId(Integer userId);
}
