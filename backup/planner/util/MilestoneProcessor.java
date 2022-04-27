package com.aktimetrix.products.svm.ciq.cdmpc.planner.util;

import com.aktimetrix.products.svm.ciq.cdmpc.model.Milestone;
import com.aktimetrix.products.svm.core.model.StepInstance;
import com.aktimetrix.products.svm.core.service.StepInstanceService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.TreeSet;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.collectingAndThen;
import static java.util.stream.Collectors.toCollection;

@Component
public class MilestoneProcessor {

    private final StepInstanceService stepInstanceService;

    @Autowired
    public MilestoneProcessor(StepInstanceService stepInstanceService) {
        this.stepInstanceService = stepInstanceService;
    }

    /**
     * Removes the Duplicates from the stepInstances
     *
     * @param stepInstances
     * @return
     */
    public List<StepInstance> removeDuplicates(List<StepInstance> stepInstances) {

        final ArrayList<StepInstance> list = stepInstances.stream()
                .collect(collectingAndThen(toCollection(() -> new TreeSet<>(new Comparator<StepInstance>() {
                    @Override
                    public int compare(StepInstance o1, StepInstance o2) {
                        if ("S".equalsIgnoreCase(o1.getGroupCode())) {
                            if (o1.getStepCode().equalsIgnoreCase(o2.getStepCode())) {
                                return 0;
                            }
                        } else if ("F".equalsIgnoreCase(o1.getGroupCode())) {
                            if (o1.getStepCode().equalsIgnoreCase(o2.getStepCode())) {
                                // compare step code and metadata
                                String flightNumber1 = (String) o1.getMetadata().get("flightNumber");
                                String flightDate1 = (String) o1.getMetadata().get("flightDate");

                                String flightNumber2 = (String) o2.getMetadata().get("flightNumber");
                                String flightDate2 = (String) o2.getMetadata().get("flightDate");
                                if (flightNumber1.equalsIgnoreCase(flightNumber2) && flightDate1.equalsIgnoreCase(flightDate2)) {
                                    return 0;
                                }
                            }
                        }
                        return 1;
                    }
                })), ArrayList::new));
        return list;
    }

    /**
     * Prepares the CDPM-C's Milestone objects
     *
     * @param stepInstances
     * @return
     */
    public List<Milestone> createMilestones(List<StepInstance> stepInstances) {
        List<Milestone> milestones = new ArrayList<>();

        stepInstances.stream().map(si -> {
            Milestone milestone = new Milestone();
            // TODO prepare the milestone object
            return milestone;
        }).collect(Collectors.toList());
        return milestones;
    }

    /**
     * @param steps
     * @return
     */
    public List<StepInstance> filterPlannedMeasurements(List<StepInstance> steps) {

        for (StepInstance step : steps) {
            if (step.getMeasurements() != null && !step.getMeasurements().isEmpty()) {
                final List<MeasurementInstance> plannedMeasurements = step.getMeasurements().stream()
                        .filter(m -> "P".equalsIgnoreCase(m.getType())).collect(Collectors.toList());
                // override with planned Measurements
                step.setMeasurements(plannedMeasurements);
            } else {
                step.setMeasurements(new ArrayList<MeasurementInstance>());
            }
        }
        return steps;
    }
}
