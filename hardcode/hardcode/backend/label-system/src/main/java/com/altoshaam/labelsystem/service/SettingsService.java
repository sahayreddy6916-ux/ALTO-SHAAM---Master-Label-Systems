package com.altoshaam.labelsystem.service;

import com.altoshaam.labelsystem.dto.SettingsRequest;
import com.altoshaam.labelsystem.dto.SettingsResponse;
import com.altoshaam.labelsystem.entity.AppSetting;
import com.altoshaam.labelsystem.exception.LabelValidationException;
import com.altoshaam.labelsystem.repository.AppSettingRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class SettingsService {

    private static final long SETTINGS_ID = 1L;

    private final AppSettingRepository repository;

    public SettingsService(AppSettingRepository repository) {
        this.repository = repository;
    }

    @Transactional(readOnly = true)
    public SettingsResponse get() {
        return toResponse(repository.findById(SETTINGS_ID).orElseGet(this::defaultSettings));
    }

    @Transactional
    public SettingsResponse update(SettingsRequest request) {
        String supplierId = request.supplierId().trim();
        if (!supplierId.matches("\\d{1,14}")) {
            throw new LabelValidationException(List.of(
                    "Supplier ID must contain 1 to 14 numeric digits."
            ));
        }

        AppSetting setting = repository.findById(SETTINGS_ID).orElseGet(this::defaultSettings);
        setting.setId(SETTINGS_ID);
        setting.setSupplierId(supplierId);
        setting.setSupplierName(value(request.supplierName()));
        setting.setShipFrom(value(request.shipFrom()));
        setting.setShipTo(value(request.shipTo()));
        setting.setPackingListSource(value(request.packingListSource()).isBlank() ? "AUTO" : value(request.packingListSource()));
        setting.setDefaultRevisionLevel(value(request.defaultRevisionLevel()));
        return toResponse(repository.save(setting));
    }

    private AppSetting defaultSettings() {
        AppSetting setting = new AppSetting();
        setting.setId(SETTINGS_ID);
        setting.setSupplierId("");
        setting.setSupplierName("");
        setting.setShipFrom("");
        setting.setShipTo("");
        setting.setPackingListSource("AUTO");
        setting.setDefaultRevisionLevel("");
        return setting;
    }

    private SettingsResponse toResponse(AppSetting s) {
        return new SettingsResponse(
                s.getId(), s.getSupplierId(), s.getSupplierName(), s.getShipFrom(),
                s.getShipTo(), s.getPackingListSource(), s.getDefaultRevisionLevel()
        );
    }

    private String value(String input) {
        return input == null ? "" : input.trim();
    }
}
