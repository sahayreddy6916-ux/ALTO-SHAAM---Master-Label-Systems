package com.altoshaam.labelsystem.controller;

import com.altoshaam.labelsystem.dto.SettingsRequest;
import com.altoshaam.labelsystem.dto.SettingsResponse;
import com.altoshaam.labelsystem.service.SettingsService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/settings")
public class SettingsController {

    private final SettingsService settingsService;

    public SettingsController(SettingsService settingsService) {
        this.settingsService = settingsService;
    }

    @GetMapping
    public SettingsResponse get() {
        return settingsService.get();
    }

    @PutMapping
    public SettingsResponse update(@Valid @RequestBody SettingsRequest request) {
        return settingsService.update(request);
    }
}
