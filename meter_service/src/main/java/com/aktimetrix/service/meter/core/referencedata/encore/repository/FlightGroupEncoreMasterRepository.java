package com.aktimetrix.service.meter.core.referencedata.encore.repository;

import com.aktimetrix.service.meter.core.referencedata.encore.model.FlightGroupEncoreMaster;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public interface FlightGroupEncoreMasterRepository extends MongoRepository<FlightGroupEncoreMaster, String> {

    /**
     * returns the Flight Group matching the Flight No.
     *
     * @param flightNo
     * @return
     */
    @Query("{ 'flightNo' : ?0 }")
    public List<FlightGroupEncoreMaster> findByFlightNo(String flightNo);


    /**
     * returns the Flights matching the Flight group code
     *
     * @param flightGroupCode
     * @return
     */
    @Query("{ 'flightGroupCode' : ?0 }")
    public List<FlightGroupEncoreMaster> findByFlightGroupCode(String flightGroupCode);

    /**
     * @param flightNo
     * @param flightGroupCode
     * @return
     */
    @Query("{ 'flightNo' : ?0, 'flightGroupCode' : ?1 }")
    List<FlightGroupEncoreMaster> findByFlightNoAndFlightGroupCode(String flightNo, String flightGroupCode);
}
