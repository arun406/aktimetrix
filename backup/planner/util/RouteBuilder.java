package com.aktimetrix.products.svm.ciq.cdmpc.planner.util;

import com.aktimetrix.products.svm.core.model.StepInstance;
import lombok.AllArgsConstructor;
import lombok.Data;
import org.bson.Document;
import org.jgrapht.GraphPath;
import org.jgrapht.alg.shortestpath.AllDirectedPaths;
import org.jgrapht.graph.DefaultWeightedEdge;
import org.jgrapht.graph.DirectedWeightedMultigraph;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * Builds the Route using JGraphT
 */
@Component
public class RouteBuilder {

    private static final Logger logger = LoggerFactory.getLogger(RouteBuilder.class);

    public static void main(String[] args) {
        RouteBuilder routeBuilder = new RouteBuilder();

        List<Document> itineraries = new ArrayList<>();
        Document itinerary = new Document();
        itinerary.put("srlNumber", 1);
        itinerary.put("boardPoint", "DXB");
        itinerary.put("offPoint", "BOM");
        itinerary.put("flightNumber", "EK1122");
        itinerary.put("flightDate", "01-01-2020");
        itineraries.add(itinerary);

        itinerary = new Document();
        itinerary.put("srlNumber", 2);
        itinerary.put("boardPoint", "BOM");
        itinerary.put("offPoint", "DEL");
        itinerary.put("flightNumber", "EK2233");
        itinerary.put("flightDate", "01-01-2020");

        itineraries.add(itinerary);

        itinerary = new Document();
        itinerary.put("srlNumber", 3);
        itinerary.put("boardPoint", "BOM");
        itinerary.put("offPoint", "HYD");
        itinerary.put("flightNumber", "EK3344");
        itinerary.put("flightDate", "02-01-2020");

        itineraries.add(itinerary);

        itinerary = new Document();
        itinerary.put("srlNumber", 4);
        itinerary.put("boardPoint", "BOM");
        itinerary.put("offPoint", "DEL");
        itinerary.put("flightNumber", "EK4455");
        itinerary.put("flightDate", "03-01-2020");

        itineraries.add(itinerary);

        itinerary = new Document();
        itinerary.put("srlNumber", 5);
        itinerary.put("boardPoint", "HYD");
        itinerary.put("offPoint", "DEL");
        itinerary.put("flightNumber", "EK5566");
        itinerary.put("flightDate", "03-01-2020");

        itineraries.add(itinerary);

        final DirectedWeightedMultigraph<Node, DefaultWeightedEdge> routeGraph =
                routeBuilder.buildRouteGraph(itineraries, "DXB", "DEL");

        final List<GraphPath<Node, DefaultWeightedEdge>> allPaths = routeBuilder.getAllPaths(routeGraph, "DXB", "DEL");

        routeBuilder.printAllPaths(allPaths);

    }

    /**
     * prints the paths
     *
     * @param allPaths
     */
    private void printAllPaths(List<GraphPath<Node, DefaultWeightedEdge>> allPaths) {
        allPaths.stream().forEach(graphPath -> {
            graphPath.getEdgeList().stream().forEach(edge -> {
                final FlightEdge flightEdge = (FlightEdge) edge;
                final Node source = (Node) flightEdge.getSource();
                final Node destination = (Node) flightEdge.getTarget();
                logger.info(String.format("Source: %s , Flight Number:  %s, Flight Date: %s , Destination: %s", new String[]{source.getAirport(), flightEdge.getFlightNumber(),
                        flightEdge.getFlightDate(), destination.getAirport()}));
            });
        });
    }

    /**
     * Get all the paths.
     *
     * @param routeGraph
     * @param origin
     * @param destination
     * @return
     */
    private List<GraphPath<Node, DefaultWeightedEdge>> getAllPaths(DirectedWeightedMultigraph<Node, DefaultWeightedEdge>
                                                                           routeGraph, String origin, String destination) {
        AllDirectedPaths<Node, DefaultWeightedEdge> dijkstraAlg = new AllDirectedPaths<>(routeGraph);

        final List<GraphPath<Node, DefaultWeightedEdge>> allPaths = dijkstraAlg.getAllPaths(
                new Node(origin), new Node(destination), true, null);
        return allPaths;
    }

    /**
     * Builds the Route Graph
     *
     * @param itineraries
     * @param origin
     * @param destination
     * @return
     */
    private DirectedWeightedMultigraph<Node, DefaultWeightedEdge> buildRouteGraph(List<Document> itineraries,
                                                                                  String origin, String destination) {

        if (itineraries == null || itineraries.isEmpty()) {
            throw new RuntimeException("Unable to build no itineraries found");
        }

        DirectedWeightedMultigraph<Node, DefaultWeightedEdge> directedMultigraph = new DirectedWeightedMultigraph<>(DefaultWeightedEdge.class);

        logger.info("Origin Airport Code : " + origin + ", Destination Airport Code: " + destination);
        for (Document itinerary : itineraries) {
            String boardPoint = (String) itinerary.get("boardPoint");
            String offPoint = (String) itinerary.get("offPoint");
            String flightDate = (String) itinerary.get("flightDate");
            String flightNumber = (String) itinerary.get("flightNumber");
            logger.info(String.format("Board point: %s , Off Point: %s, Flight Number: %s , Flight Date: %s",
                    new String[]{boardPoint, offPoint, flightNumber, flightDate}));
            final Node bpNode = new Node(boardPoint);
            final Node opNode = new Node(offPoint);
            directedMultigraph.addVertex(bpNode);
            directedMultigraph.addVertex(opNode);
            directedMultigraph.addEdge(bpNode, opNode, new FlightEdge(flightNumber, flightDate));
        }

        return directedMultigraph;
    }


    /**
     * @param itineraries
     * @param origin
     * @param destination
     */
    public void getAllRouteMaps(List<Document> itineraries, List<StepInstance> steps, String origin, String destination) {

        final DirectedWeightedMultigraph<Node, DefaultWeightedEdge> routeGraph = this.buildRouteGraph(itineraries, origin, destination);
        final List<GraphPath<Node, DefaultWeightedEdge>> allPaths = this.getAllPaths(routeGraph, origin, destination);
        int counter = 1;
        Map<String, List<StepInstance>> routeMaps = new HashMap<>();
        for (GraphPath<Node, DefaultWeightedEdge> path : allPaths) {
            List<StepInstance> routeSteps = new ArrayList<>();
            for (DefaultWeightedEdge defaultWeightedEdge : path.getEdgeList()) {
                FlightEdge flightEdge = (FlightEdge) defaultWeightedEdge;
                Node bpNode = (Node) flightEdge.getSource();
                String sourceAirport = bpNode.getAirport();
                logger.info("source sourceAirport : " + sourceAirport);
                final List<StepInstance> sourceSteps = steps.stream()
                        .filter(stepInstance ->
                                stepInstance.getLocationCode().equalsIgnoreCase(sourceAirport)
                        ).filter(stepInstance -> isApplicableStep(flightEdge, stepInstance))
                        .collect(Collectors.toList());
                routeSteps.addAll(sourceSteps);
                Node opNode = (Node) flightEdge.getTarget();
                String destinationAirport = opNode.getAirport();
                final List<StepInstance> destinationSteps = steps.stream()
                        .filter(stepInstance ->
                                stepInstance.getLocationCode().equalsIgnoreCase(destinationAirport)
                        ).filter(stepInstance -> isApplicableStep(flightEdge, stepInstance))
                        .collect(Collectors.toList());

                routeSteps.addAll(destinationSteps);
            }
            routeMaps.put(counter + "", routeSteps);
            counter++;
            logger.info("............ start of sequence of steps...............");

            routeSteps.stream().forEach(stepInstance -> logger.info(stepInstance.getStepCode() + '-' + stepInstance.getLocationCode()));

            logger.info("............ end of sequence of steps...............");
        }
    }

    private boolean isApplicableStep(FlightEdge flightEdge, StepInstance stepInstance) {
        if (stepInstance.getGroupCode().equalsIgnoreCase("S")) {
            return true;
        } else if (stepInstance.getGroupCode().equalsIgnoreCase("F")) {
            final String flightNumber = (String) stepInstance.getMetadata().get("flightNumber");
            final String flightDate = (String) stepInstance.getMetadata().get("flightDate");
            if (flightNumber.equalsIgnoreCase(flightEdge.getFlightNumber())
                    && flightDate.equalsIgnoreCase(flightEdge.getFlightDate())) {
                return true;
            }
        }
        return false;
    }

    @Data
    @AllArgsConstructor
    class Node {
        private String airport;

        @Override
        public boolean equals(Object o) {
            if (this == o) return true;
            if (o == null || getClass() != o.getClass()) return false;
            Node node1 = (Node) o;
            return airport.equals(node1.airport);
        }

        @Override
        public int hashCode() {
            return Objects.hash(airport);
        }
    }


    @Data
    @AllArgsConstructor
    class FlightEdge extends DefaultWeightedEdge {
        private String flightNumber;
        private String flightDate;

        @Override
        protected Object getSource() {
            return super.getSource();
        }

        @Override
        protected Object getTarget() {
            return super.getTarget();
        }

        @Override
        public String toString() {
            return "Itinerary{" +
                    flightNumber + '-' +
                    flightDate +
                    '}';
        }
    }
}

