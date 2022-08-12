package com.aktimetrix.aktimetrix.reference.data.repository;

import com.aktimetrix.aktimetrix.reference.data.model.Line;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface LinesRepository extends MongoRepository<Line, String> {

    /**
     * @param tenant
     * @param airport
     * @param forwarderCode
     * @param acCategory
     * @param eFreightIndicator
     * @return
     */
    @Query("{ 'airline': ?0 , 'airport' : ?1 , 'forwarderCode' : ?2, 'acCategory' : ?3 , 'eFreightInd': ?4 }")
    List<Line> findBy(String tenant, String airport, String forwarderCode, String acCategory, String eFreightIndicator);
}
