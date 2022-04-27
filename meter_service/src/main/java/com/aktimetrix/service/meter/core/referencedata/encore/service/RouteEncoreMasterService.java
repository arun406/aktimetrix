package com.aktimetrix.service.meter.core.referencedata.encore.service;

import com.aktimetrix.service.meter.core.referencedata.encore.model.RouteEncoreMaster;
import com.aktimetrix.service.meter.core.referencedata.encore.repository.RouteEncoreMasterRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class RouteEncoreMasterService {

    @Autowired
    private RouteEncoreMasterRepository repository;


    /**
     * @return
     */
    public List<RouteEncoreMaster> list() {
        return repository.findAll();
    }

    /**
     * @param encoreMaster
     * @return
     */
    public RouteEncoreMaster add(RouteEncoreMaster encoreMaster) {
        repository.save(encoreMaster);
        return encoreMaster;
    }

    /**
     * @param tenant
     * @param airline
     * @param origin
     * @param destination
     * @param forwarderCode
     * @return
     */
    public RouteEncoreMaster getRouteByAirlineCodeAndOriginAndDestinationAndForwarderCode(String tenant, String airline, String origin,
                                                                                          String destination, String forwarderCode) {
        final List<RouteEncoreMaster> list = this.repository.findByAirlineAndOriginAndDestinationAndForwarder(tenant, airline, origin,
                destination, forwarderCode);
        if (list != null && list.size() == 1) {
            return list.get(0);
        }
        return null;
    }

}
