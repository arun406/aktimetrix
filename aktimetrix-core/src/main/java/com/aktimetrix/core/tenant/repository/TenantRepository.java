package com.aktimetrix.core.tenant.repository;

import com.aktimetrix.core.tenant.model.TenantConfigurations;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Component;

@Component
public interface TenantRepository extends MongoRepository<TenantConfigurations, String> {

    /**
     * returns the first tenant matching the parameter
     *
     * @param tenantCode
     * @return
     */
    public TenantConfigurations findByCode(String tenantCode);
}
