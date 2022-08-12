package com.aktimetrix.aktimetrix.reference.data.repository;

import com.aktimetrix.aktimetrix.reference.data.model.ProductGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface ProductGroupRepository extends MongoRepository<ProductGroup, String> {
}
