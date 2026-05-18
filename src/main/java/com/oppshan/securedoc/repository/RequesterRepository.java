package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Requester;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

@Repository
public interface RequesterRepository extends BasicRepository<Requester, Long> {
}
