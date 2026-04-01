package com.thinkcode.transportbackend.domain;

import com.thinkcode.transportbackend.common.BaseEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;

@Entity
public class Company extends BaseEntity {

    @Column(nullable = false, unique = true)
    private String name;

    @Column(nullable = false, unique = true)
    private String code;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = normalize(name);
    }

    public String getCode() {
        return code;
    }

    public void setCode(String code) {
        this.code = normalize(code);
    }

    private String normalize(String value) {
        return value == null ? null : value.trim().toUpperCase();
    }
}

