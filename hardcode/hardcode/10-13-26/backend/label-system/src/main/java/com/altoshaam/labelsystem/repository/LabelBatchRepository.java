package com.altoshaam.labelsystem.repository;

import com.altoshaam.labelsystem.entity.LabelBatch;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Optional;

public interface LabelBatchRepository extends JpaRepository<LabelBatch, Long> {

    Page<LabelBatch> findAllByOrderByCreatedAtDesc(Pageable pageable);

    @EntityGraph(attributePaths = {"items", "items.generatedLabels"})
    Optional<LabelBatch> findWithItemsById(Long id);
}
