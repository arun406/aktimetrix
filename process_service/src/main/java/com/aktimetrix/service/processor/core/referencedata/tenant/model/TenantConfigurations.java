package com.aktimetrix.service.processor.core.referencedata.tenant.model;

import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "tenantConfigurations")
public class TenantConfigurations {
    private String code;
    private String name;
    private License license;
    private List<Property> properties;
    private List<ProductConfigurations> products;
}
