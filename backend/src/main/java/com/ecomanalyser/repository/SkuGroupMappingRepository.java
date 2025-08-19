package com.ecomanalyser.repository;

import com.ecomanalyser.domain.SkuGroupMappingEntity;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface SkuGroupMappingRepository extends JpaRepository<SkuGroupMappingEntity, Long> {
    
    Optional<SkuGroupMappingEntity> findBySku(String sku);
    
    List<SkuGroupMappingEntity> findBySkuGroupId(Long groupId);
    
    @Query("SELECT sgm.sku FROM SkuGroupMappingEntity sgm WHERE sgm.skuGroup.id = :groupId")
    List<String> findSkusByGroupId(@Param("groupId") Long groupId);
    
    @Query("SELECT sgm.skuGroup.groupName FROM SkuGroupMappingEntity sgm WHERE sgm.sku = :sku")
    Optional<String> findGroupNameBySku(@Param("sku") String sku);
    
    @Query("SELECT COUNT(sgm) FROM SkuGroupMappingEntity sgm WHERE sgm.skuGroup.id = :groupId")
    Long countSkusByGroupId(@Param("groupId") Long groupId);
    
    @Query("SELECT sgm FROM SkuGroupMappingEntity sgm JOIN FETCH sgm.skuGroup WHERE sgm.skuGroup IS NOT NULL")
    List<SkuGroupMappingEntity> findAllWithGroupDetails();
}
