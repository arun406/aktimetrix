package com.aktimetrix.service.meter.core.referencedata.encore.service;

import com.aktimetrix.service.meter.core.referencedata.encore.model.ProductGroupEncoreMaster;
import com.aktimetrix.service.meter.core.referencedata.encore.repository.ProductGroupEncoreMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductGroupEncoreMasterService {

    @Autowired
    ProductGroupEncoreMasterRepository repository;

    /**
     * Adds a new Product Group Encore Master Record
     *
     * @param productGroupEncoreMaster
     * @return
     */
    public ProductGroupEncoreMaster add(ProductGroupEncoreMaster productGroupEncoreMaster) {
        repository.save(productGroupEncoreMaster);
        return productGroupEncoreMaster;
    }


    /**
     * returns all ProductGroupEncoreMaster
     *
     * @return
     */
    public List<ProductGroupEncoreMaster> list() {
        return repository.findAll();
    }


    /**
     * Return the product Group Code for a product Code
     *
     * @param productCode
     * @return
     */
    public String getProductGroupCode(String productCode) {

        final ProductGroupEncoreMaster productGroup = this.repository.findByProductCode(productCode);
        if (productGroup != null) {
            return productGroup.getProductGroupCode();
        }
        return null;
    }
}
