package com.altoshaam.labelsystem.repository;

import java.time.Instant;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.altoshaam.labelsystem.entity.GeneratedLabel;
import com.altoshaam.labelsystem.entity.PrintStatus;

public interface GeneratedLabelRepository extends JpaRepository<GeneratedLabel, Long> {

    /** Marks every label of a batch as printed with one UPDATE instead of loading them all. */
    @Modifying(clearAutomatically = true, flushAutomatically = true)
    @Query("""
            update GeneratedLabel g
               set g.status = :status, g.printedAt = :printedAt
             where g.labelItem.id in (select i.id from LabelItem i where i.batch.id = :batchId)
            """)
    int markBatchPrinted(
            @Param("batchId") Long batchId,
            @Param("status") PrintStatus status,
            @Param("printedAt") Instant printedAt);
}
