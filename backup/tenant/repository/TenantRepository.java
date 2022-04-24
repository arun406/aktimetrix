package com.aktimetrix.core.tenant.repository;

import com.aktimetrix.core.tenant.model.TenantConfigurations;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface TenantRepository extends MongoRepository<TenantConfigurations, String> {

    /**
     * returns the first tenant matching the parameter
     *
     * @param tenantCode
     * @return
     */
    TenantConfigurations findByCode(String tenantCode);
}
