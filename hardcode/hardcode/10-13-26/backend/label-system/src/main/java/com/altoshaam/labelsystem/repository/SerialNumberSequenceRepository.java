package com.altoshaam.labelsystem.repository;

import com.altoshaam.labelsystem.entity.SerialNumberSequence;
import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface SerialNumberSequenceRepository extends JpaRepository<SerialNumberSequence, Long> {

    @Modifying
    @Query(value = """
            INSERT INTO serial_number_sequence (supplier_id, current_sequence, updated_at)
            VALUES (:supplierId, 0, CURRENT_TIMESTAMP)
            ON CONFLICT (supplier_id) DO NOTHING
            """, nativeQuery = true)
    int ensureSequence(@Param("supplierId") String supplierId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    Optional<SerialNumberSequence> findBySupplierId(String supplierId);
}
