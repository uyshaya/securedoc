package com.oppshan.securedoc.repository;

import com.oppshan.securedoc.model.Barangay;
import jakarta.data.repository.BasicRepository;
import jakarta.data.repository.Repository;

@Repository
public interface BarangayRepository extends BasicRepository<Barangay, Long> {
}
