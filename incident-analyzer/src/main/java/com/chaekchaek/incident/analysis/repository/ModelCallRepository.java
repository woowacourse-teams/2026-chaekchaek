package com.chaekchaek.incident.analysis.repository;

import com.chaekchaek.incident.analysis.domain.ModelCall;
import java.time.Instant;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ModelCallRepository extends JpaRepository<ModelCall, Long> {
    long countByReservedAtGreaterThanEqual(Instant from);
}
