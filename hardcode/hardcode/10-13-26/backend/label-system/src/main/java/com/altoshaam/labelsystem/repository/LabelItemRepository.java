package com.altoshaam.labelsystem.repository;

import com.altoshaam.labelsystem.entity.LabelItem;
import org.springframework.data.jpa.repository.JpaRepository;

public interface LabelItemRepository extends JpaRepository<LabelItem, Long> {
}
