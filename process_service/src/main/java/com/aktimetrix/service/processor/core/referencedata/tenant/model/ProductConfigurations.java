package com.aktimetrix.service.processor.core.referencedata.tenant.model;

import lombok.Data;

import java.util.List;

@Data
public class ProductConfigurations {
    private String code;
    private String name;
    private License license;
    private List<Property> properties;
}
