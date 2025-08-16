package com.ecomanalyser.repository;

import com.ecomanalyser.domain.SkuGroupEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkuGroupRepository extends JpaRepository<SkuGroupEntity, Long> {
    
    Optional<SkuGroupEntity> findByGroupName(String groupName);
    
    List<SkuGroupEntity> findByGroupNameContainingIgnoreCase(String groupName);
    
    @Query("SELECT sg FROM SkuGroupEntity sg WHERE sg.groupName LIKE %:searchTerm% OR sg.description LIKE %:searchTerm%")
    List<SkuGroupEntity> searchByGroupNameOrDescription(@Param("searchTerm") String searchTerm);
}
