package com.example.adminwebjava.repository;

import com.example.adminwebjava.entity.MenuOrder;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface MenuOrderRepository extends JpaRepository<MenuOrder, Integer> {
    List<MenuOrder> findByOwnerProfileId(Integer ownerProfileId);
    List<MenuOrder> findByTouristId(Integer touristId);
    List<MenuOrder> findByStatus(String status);
}
