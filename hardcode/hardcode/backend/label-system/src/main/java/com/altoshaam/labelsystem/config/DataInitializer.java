package com.altoshaam.labelsystem.config;

import com.altoshaam.labelsystem.entity.AppSetting;
import com.altoshaam.labelsystem.repository.AppSettingRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class DataInitializer {

    @Bean
    CommandLineRunner initializeSettings(AppSettingRepository repository) {
        return args -> {
            if (repository.findById(1L).isEmpty()) {
                AppSetting setting = new AppSetting();
                setting.setId(1L);
                setting.setSupplierId("");
                setting.setSupplierName("");
                setting.setShipFrom("");
                setting.setShipTo("");
                setting.setPackingListSource("AUTO");
                setting.setDefaultRevisionLevel("");
                repository.save(setting);
            }
        };
    }
}
