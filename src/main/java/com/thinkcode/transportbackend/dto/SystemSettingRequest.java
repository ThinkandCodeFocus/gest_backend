package com.thinkcode.transportbackend.dto;

import java.util.UUID;

public record SystemSettingRequest(
    String settingKey,
    String settingValue,
    String description
) {}
