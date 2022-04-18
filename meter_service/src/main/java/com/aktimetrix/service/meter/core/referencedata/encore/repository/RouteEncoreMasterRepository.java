package com.aktimetrix.service.meter.core.referencedata.encore.repository;

import com.aktimetrix.service.meter.core.referencedata.encore.model.RouteEncoreMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface RouteEncoreMasterRepository extends MongoRepository<RouteEncoreMaster, String> {

    // find documents by airline

    /**
     * Lists the RouteEcoreMaster Based on the Airline Code
     *
     * @param tenant
     * @param airline
     * @return
     */
    @Query("{ 'tenant' : ?0, 'airline' : ?1 }")
    public List<RouteEncoreMaster> findByAirline(String tenant, String airline);

    /**
     * Lists the RouteEncoreMaster Based on Forwarder Code
     *
     * @param forwarder
     * @return
     */
    @Query("{ 'tenant' : ?0, 'forwarder' : ?1 }")
    public List<RouteEncoreMaster> findByForwarder(String tenant, String forwarder);

    /**
     * Lists the RouteEncore Master Based on Airline Code, Origin, Destination and Forwarder Code Combination
     *
     * @param airline
     * @param origin
     * @param destination
     * @param forwarder
     * @return
     */
    @Query("{ 'tenant' : ?0, 'airline' : ?1 , 'origin': ?2 , 'destination' : ?3 , 'forwarder': ?4 }")
    public List<RouteEncoreMaster> findByAirlineAndOriginAndDestinationAndForwarder(String tenant, String airline, String origin,
                                                                                    String destination, String forwarder);
}
