package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.DocumentTemplate;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;

@Repository
public interface DocumentTemplateRepository extends BasicRepository<DocumentTemplate, Long> {

    @Query("FROM DocumentTemplate WHERE organization.id = ?1 AND isActive = TRUE ORDER BY name")
    List<DocumentTemplate> listActiveByOrganizationId(Long organizationId);
}
