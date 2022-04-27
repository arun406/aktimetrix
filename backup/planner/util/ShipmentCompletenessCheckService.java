package com.aktimetrix.products.svm.ciq.cdmpc.planner.util;

import com.aktimetrix.products.svm.ciq.cdmpc.service.CDMPCCommonUtil;
import com.aktimetrix.products.svm.core.model.ProcessInstance;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.service.StepInstanceService;
import org.bson.Document;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 *
 */
@Component
public class ShipmentCompletenessCheckService {

    final private static Logger logger = LoggerFactory.getLogger(ShipmentCompletenessCheckService.class);

    final private StepInstanceService stepInstanceService;
    final private CDMPCCommonUtil commonUtil;

    /**
     *
     * @param stepInstanceService
     * @param commonUtil
     */
    public ShipmentCompletenessCheckService(StepInstanceService stepInstanceService, CDMPCCommonUtil commonUtil) {
        this.stepInstanceService = stepInstanceService;
        this.commonUtil = commonUtil;
    }

    /**
     * @param tenant
     * @param processInstance
     * @return
     */
    public boolean isComplete(String tenant, ProcessInstance processInstance) {
        final Map<String, Object> metadata = processInstance.getMetadata();
        String origin = (String) metadata.get("origin");
        String destination = (String) metadata.get("destination");
        int reservationPieces = (int) metadata.get("reservationPieces");
        double reservationVolume = (double) metadata.get("reservationVolume");
        double reservationWeight = (double) metadata.get("reservationWeight");

        final List<StepInstance> stepInstances = this.stepInstanceService.getStepInstancesByProcessInstanceId(tenant, processInstance.getId());
        final List<Document> itineraries = commonUtil.getItinerariesFromProcessInstance(stepInstances);

        final boolean complete = check(createDirectedMutigraph(itineraries), origin, destination, reservationPieces);
        logger.info(" is the all routes are available : " + complete);
        return complete;
    }

    /**
     * @param g
     * @param origin
     * @param destination
     * @param reservationPieces
     * @return
     */
    private boolean check(DirectedWeightedMultigraph<String, DefaultWeightedEdge> g, String origin, String destination,
                          int reservationPieces) {

        final Set<String> airports = g.vertexSet();
        logger.info("all airports in the itineraries");

        for (String airport : airports) {

            logger.info("airport : " + airport);
            // get outgoing
            final Set<DefaultWeightedEdge> outgoingEdges = g.outgoingEdgesOf(airport);
            final double outSum = outgoingEdges.stream().mapToDouble(defaultWeightedEdge -> g.getEdgeWeight(defaultWeightedEdge)).sum();
            logger.info(String.valueOf(outSum));
            // get incoming
            final Set<DefaultWeightedEdge> inComingEdges = g.incomingEdgesOf(airport);
            final double inSum = inComingEdges.stream().mapToDouble(defaultWeightedEdge -> g.getEdgeWeight(defaultWeightedEdge)).sum();
            logger.info(String.valueOf(inSum));

            if (airport.equalsIgnoreCase(origin)) {
                if (outSum != reservationPieces) {
                    return false;
                }
            } else if (airport.equalsIgnoreCase(destination)) {
                if (inSum != reservationPieces) {
                    return false;
                }
            } else {
                if (inSum != outSum) {
                    return false;
                }
            }
        }
        return true;
    }

    /**
     * @param itineraries
     * @return
     */
    private DirectedWeightedMultigraph<String, DefaultWeightedEdge> createDirectedMutigraph(List<Document> itineraries) {
        DirectedWeightedMultigraph<String, DefaultWeightedEdge> graph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);

        if (itineraries == null || itineraries.isEmpty()) {
            throw new RuntimeException("Unable to build no itineraries found");
        }

        for (Document itinerary : itineraries) {
            String boardPoint = (String) itinerary.get("boardPoint");
            String offPoint = (String) itinerary.get("offPoint");
            int pieces = ((int) itinerary.get("pieces"));
            double wt = ((double) itinerary.get("wt"));
            double vol = ((double) itinerary.get("vol"));
            String flightDate = (String) itinerary.get("flightDate");
            String flightNumber = (String) itinerary.get("flightNumber");
            graph.addVertex(boardPoint);
            graph.addVertex(offPoint);
            final DefaultWeightedEdge edge = graph.addEdge(boardPoint, offPoint);
            graph.setEdgeWeight(edge, pieces);
        }

        return graph;
    }
}