package com.aktimetrix.service.meter.core.referencedata.encore.repository;

import com.aktimetrix.service.meter.core.referencedata.encore.model.ProductGroupEncoreMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface ProductGroupEncoreMasterRepository extends MongoRepository<ProductGroupEncoreMaster, String> {

    /**
     * returns the Product Group matching the product Code.
     *
     * @param productCode
     * @return
     */
    @Query("{ 'productCode' : ?0}")
    public ProductGroupEncoreMaster findByProductCode(String productCode);


    /**
     * returns the products matching the product group code
     *
     * @param productGroupCode
     * @return
     */
    @Query("{ 'productGroupCode' : ?1 }")
    public List<ProductGroupEncoreMaster> findByProductGroupCode(String productGroupCode);
}
