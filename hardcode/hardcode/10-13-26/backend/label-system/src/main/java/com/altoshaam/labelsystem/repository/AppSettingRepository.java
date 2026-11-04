package com.altoshaam.labelsystem.repository;

import com.altoshaam.labelsystem.entity.AppSetting;
import org.springframework.data.jpa.repository.JpaRepository;

public interface AppSettingRepository extends JpaRepository<AppSetting, Long> {
}
