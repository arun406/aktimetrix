package com.aktimetrix.aktimetrix.reference.data.repository;

import com.aktimetrix.aktimetrix.reference.data.model.Route;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface RouteRepository extends MongoRepository<Route, String> {
}
