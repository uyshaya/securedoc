package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.common.StatefulWriteRepository;
import com.oppshan.securedoc.model.DocumentTemplate;
import jakarta.data.repository.CrudRepository;
import jakarta.data.repository.Query;
import jakarta.data.repository.Repository;

import java.util.List;
import java.util.UUID;

@Repository
public interface DocumentTemplateRepository
        extends CrudRepository<DocumentTemplate, UUID>, StatefulWriteRepository<DocumentTemplate> {

    @Query("FROM DocumentTemplate WHERE organization.id = ?1 AND active = TRUE ORDER BY name")
    List<DocumentTemplate> listActiveByOrganizationId(UUID organizationId);
}
