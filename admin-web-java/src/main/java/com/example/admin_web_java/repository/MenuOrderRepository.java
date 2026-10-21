package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.MenuOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MenuOrderRepository extends JpaRepository {
    List findByOwnerProfileId(Integer ownerProfileId);
    List findByTouristId(Integer touristId);
    List findByStatus(String status);
}