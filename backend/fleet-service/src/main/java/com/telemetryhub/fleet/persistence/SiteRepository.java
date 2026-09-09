package com.telemetryhub.fleet.persistence;

import com.telemetryhub.fleet.domain.Site;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface SiteRepository extends JpaRepository<Site, UUID> {

    Page<Site> findByTenantId(UUID tenantId, Pageable pageable);

    List<Site> findByTenantIdAndActiveTrue(UUID tenantId);

    Optional<Site> findByIdAndTenantId(UUID id, UUID tenantId);
}