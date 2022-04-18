package com.aktimetrix.service.processor.core.referencedata.tenant.service;

import com.aktimetrix.service.processor.core.referencedata.tenant.model.ProductConfigurations;
import com.aktimetrix.service.processor.core.referencedata.tenant.model.Property;
import com.aktimetrix.service.processor.core.referencedata.tenant.repository.TenantRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class TenantService {

    @Autowired
    TenantRepository repository;

    /**
     * Return all product's properties
     *
     * @param tenantCode
     * @param productCode
     * @return
     */
    public List<Property> listProductProperties(String tenantCode, String productCode) {
        ProductConfigurations productConfigurations = this.repository.findByCode(tenantCode)
                .getProducts()
                .stream()
                .filter(pc -> pc.getCode().equals(productCode))
                .findFirst().orElse(null);
        if (productConfigurations != null) {
            return productConfigurations.getProperties();
        }
        return new ArrayList<Property>();
    }

    /**
     * @param tenantCode
     * @param productCode
     * @param propertyCode
     * @return
     */
    public String getValue(String tenantCode, String productCode, String propertyCode) {

        Property property = this.repository.findByCode(tenantCode)
                .getProducts().stream()
                .filter(pc -> pc.getCode().equals(productCode))
                .flatMap(pc -> pc.getProperties().stream())
                .filter(p -> p.getCode().equals(propertyCode))
                .findFirst().orElse(null);
        if (property != null)
            return property.getValue();
        return null;
    }
}
