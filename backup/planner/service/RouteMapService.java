package com.aktimetrix.products.svm.ciq.cdmpc.planner.service;

import com.aktimetrix.products.svm.ciq.cdmpc.repository.RouteMapRepository;
import org.bson.types.ObjectId;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class RouteMapService {

    @Autowired
    private RouteMapRepository repository;

    /**
     * Saves the route map
     *
     * @param routeMap
     */
    public RouteMap saveRouteMap(RouteMap routeMap) {
        return this.repository.save(routeMap);
    }

    /**
     * @param tenant
     * @param planId
     */
    public RouteMap getRouteMapByPlanId(String tenant, ObjectId planId) {
        return this.repository.findByPlanId(tenant, planId);
    }
}
