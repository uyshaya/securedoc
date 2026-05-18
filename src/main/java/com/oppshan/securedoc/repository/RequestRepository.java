package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Request;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

@Repository
public interface RequestRepository extends BasicRepository<Request, Long> {
}
