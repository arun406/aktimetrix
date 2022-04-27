package com.aktimetrix.service.processor.ciq.cdmpc.impl;

import com.aktimetrix.core.api.DefinitionProvider;
import com.aktimetrix.core.exception.DefinitionNotFoundException;
import com.aktimetrix.core.referencedata.model.StepDefinition;
import com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects.Cargo;
import com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects.Itinerary;
import com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects.StationInfo;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.lang3.StringUtils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.Comparator.comparing;
import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Slf4j
public class CiQStepDefinitionProvider implements DefinitionProvider<StepDefinition> {

    private List<StepDefinition> steps;
    private Itinerary itinerary;
    private Cargo cargo;

    public CiQStepDefinitionProvider(List<StepDefinition> steps, Cargo cargo, Itinerary itinerary) {
        this.steps = steps;
        this.cargo = cargo;
        this.itinerary = itinerary;
    }

    /**
     * returns the functional context
     *
     * @param airport     airport
     * @param origin      origin
     * @param destination destination
     * @return functional context
     */
    private String getFnCtx(StationInfo airport, StationInfo origin, StationInfo destination) {
        if (StringUtils.equalsIgnoreCase(airport.getCode(), origin.getCode())) {
            return "E";
        } else if (StringUtils.equalsIgnoreCase(airport.getCode(), destination.getCode())) {
            return "I";
        } else {
            return "T";
        }
    }

    /**
     * Returns the applicable step definitions for the booking event
     *
     * @return step definition collection
     */
    @Override
    public List<StepDefinition> getDefinitions() throws DefinitionNotFoundException {

        String finalBoardPointFnCtx = getFnCtx(itinerary.getBoardPoint(), cargo.getOrigin(), cargo.getDestination());
        String finalOffPointFnCtx = getFnCtx(itinerary.getOffPoint(), cargo.getOrigin(), cargo.getDestination());
        log.info("Boarding Point Functional Context : " + finalBoardPointFnCtx + ", Off Point Functional Context : " + finalOffPointFnCtx);
        // remove the duplicates also
        final List<StepDefinition> stepDefinitions = steps.stream()
                .filter(sd ->
                        finalBoardPointFnCtx.equalsIgnoreCase(sd.getFunctionalCtxCode()) ||
                                finalOffPointFnCtx.equalsIgnoreCase(sd.getFunctionalCtxCode()))
                .collect(collectingAndThen(toCollection(() -> new TreeSet<>(comparing(StepDefinition::getStepCode))),
                        ArrayList::new));

        for (Iterator<StepDefinition> it = stepDefinitions.iterator(); it.hasNext(); ) {
            StepDefinition sd = it.next();
            if (StringUtils.equalsIgnoreCase("E", finalBoardPointFnCtx) && StringUtils.equalsIgnoreCase("T", finalOffPointFnCtx)) {
                if (StringUtils.equalsIgnoreCase(sd.getStepCode(), "DEP-T")) {
                    it.remove();
                }
            } else if (StringUtils.equalsIgnoreCase("T", finalBoardPointFnCtx) && StringUtils.equalsIgnoreCase("I", finalOffPointFnCtx)) {
                if (StringUtils.equalsIgnoreCase(sd.getStepCode(), "ARR-T")
                        || StringUtils.equalsIgnoreCase(sd.getStepCode(), "RCF-T")) {
                    it.remove();
                }
            }
        }
        log.info(String.format("step codes : %s", stepDefinitions.stream()
                .map(StepDefinition::getStepCode)
                .collect(Collectors.joining(","))));

        return stepDefinitions;
    }
}
