package com.aktimetrix.aktimetrix.reference.data.repository;

import com.aktimetrix.aktimetrix.reference.data.model.FlightGroup;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.data.mongodb.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface FlightGroupRepository extends MongoRepository<FlightGroup, String> {

    /**
     * returns the Flight Group matching the Flight Number.
     *
     * @param flightNumber
     * @return
     */
    @Query("{ 'flightNumber' : ?0 }")
    public List<FlightGroup> findByFlightNo(String flightNumber);


    /**
     * returns the Flights matching the Flight group code
     *
     * @param flightGroupCode
     * @return
     */
    @Query("{ 'flightGroupCode' : ?0 }")
    public List<FlightGroup> findByFlightGroupCode(String flightGroupCode);

    /**
     * @param flightNumber
     * @param flightGroupCode
     * @return
     */
    @Query("{ 'flightNumber' : ?0, 'flightGroupCode' : ?1 }")
    List<FlightGroup> findByFlightNumberAndFlightGroupCode(String flightNumber, String flightGroupCode);
}
