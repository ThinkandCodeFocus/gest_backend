package com.thinkcode.transportbackend.service;

import com.thinkcode.transportbackend.entity.SystemSetting;
import com.thinkcode.transportbackend.repository.SystemSettingRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.UUID;

@Service
public class SystemSettingService {

    private final SystemSettingRepository systemSettingRepository;

    public SystemSettingService(SystemSettingRepository systemSettingRepository) {
        this.systemSettingRepository = systemSettingRepository;
    }

    public List<SystemSetting> getAll() {
        return systemSettingRepository.findAll();
    }

    public SystemSetting getByKey(String key) {
        return systemSettingRepository.findBySettingKey(key)
                .orElse(null);
    }

    public SystemSetting save(SystemSetting setting) {
        return systemSettingRepository.save(setting);
    }

    public SystemSetting update(UUID id, SystemSetting setting) {
        return systemSettingRepository.findById(id)
                .map(existing -> {
                    existing.setSettingValue(setting.getSettingValue());
                    existing.setDescription(setting.getDescription());
                    return systemSettingRepository.save(existing);
                })
                .orElse(null);
    }

    public void delete(UUID id) {
        systemSettingRepository.deleteById(id);
    }
}
