package com.aktimetrix.service.processor.ciq.cdmpc.impl;

import com.aktimetrix.core.model.StepInstance;
import org.bson.Document;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Component
public class ItineraryProvider {

    /**
     * prepare metadata
     *
     * @param stepInstances step instances
     * @return metadata
     */
    public List<Document> getItineraries(List<StepInstance> stepInstances) {
        List<Document> itineraries = null;
        // fetch all step instances for the give processInstanceId
        // get the bp and op pair from the step instances

        if (stepInstances != null && !stepInstances.isEmpty()) {
            AtomicInteger counter = new AtomicInteger(0);
            itineraries = stepInstances.stream()
                    .filter(si -> si.getStepCode().equalsIgnoreCase("BKD") && si.getMetadata() != null)
                    .map(StepInstance::getMetadata)
                    .map(d -> {
                        Document document = new Document();
                        document.put("boardPoint", d.get("boardPoint"));
                        document.put("offPoint", d.get("offPoint"));
                        document.put("flightNumber", d.get("flightNumber"));
                        document.put("flightDate", d.get("flightDate"));
                        document.put("pieces", d.get("pieces"));
                        document.put("wt", d.get("wt"));
                        document.put("wtUnit", d.get("wtUnit"));
                        document.put("vol", d.get("vol"));
                        document.put("volUnit", d.get("volUnit"));
                        return document;
                    })
                    .distinct()
                    .collect(Collectors.toList());
        }
        return itineraries;
    }
}
