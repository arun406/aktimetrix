package com.aktimetrix.service.processor.ciq.cdmpc.service;

import com.aktimetrix.service.processor.ciq.cdmpc.impl.ItineraryProvider;
import com.aktimetrix.service.processor.core.api.PostProcessor;
import com.aktimetrix.service.processor.core.api.ProcessContext;
import com.aktimetrix.service.processor.core.model.ProcessInstance;
import com.aktimetrix.service.processor.core.model.StepInstance;
import com.aktimetrix.service.processor.core.service.StepInstanceService;
import lombok.AllArgsConstructor;
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
@AllArgsConstructor
public class CiQCompletenessCheckService implements PostProcessor {

    final private static Logger logger = LoggerFactory.getLogger(CiQCompletenessCheckService.class);

    final private StepInstanceService stepInstanceService;
    final private ItineraryProvider itineraryProvider;

    /**
     * @param tenant          tenant
     * @param processInstance process instance
     * @return boolean
     */
    public boolean isComplete(String tenant, ProcessInstance processInstance) {
        final Map<String, Object> metadata = processInstance.getMetadata();
        String origin = (String) metadata.get("origin");
        String destination = (String) metadata.get("destination");
        int reservationPieces = (int) metadata.get("reservationPieces");
        double reservationVolume = (double) metadata.get("reservationVolume");
        double reservationWeight = (double) metadata.get("reservationWeight");

        final List<StepInstance> stepInstances = this.stepInstanceService.getStepInstancesByProcessInstanceId(tenant, processInstance.getId());
        final List<Document> itineraries = itineraryProvider.getItineraries(stepInstances);

        final boolean complete = check(createDirectedMultigraph(itineraries), origin, destination, reservationPieces);
        logger.info(" is the all routes are available : " + complete);
        return complete;
    }

    /**
     * @param g
     * @param origin            origin
     * @param destination       destination
     * @param reservationPieces reservation pieces
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
     * @param itineraries itinerary
     * @return
     */
    private DirectedWeightedMultigraph<String, DefaultWeightedEdge> createDirectedMultigraph(List<Document> itineraries) {
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

    @Override
    public void postProcess(ProcessContext context) {
        final ProcessInstance processInstance = context.getProcessInstance();
        final String tenant = context.getTenant();
        final boolean complete = isComplete(tenant, processInstance);
        logger.debug("Process Status : {}", complete ? "COMPLETE" : "NOTCOMPLETE");
    }

}