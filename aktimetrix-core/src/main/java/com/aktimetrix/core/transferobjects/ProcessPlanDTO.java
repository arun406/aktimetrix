package com.aktimetrix.core.transferobjects;

import com.aktimetrix.core.transferobjects.ProcessInstanceDTO;
import lombok.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

/**
 * Plan is series of Steps with planned measurements map order by some criteria
 *
 * @author Arun kumar K
 */
@Data
@NoArgsConstructor
@ToString
@Builder
@AllArgsConstructor
public class ProcessPlanDTO {
    private String id;
    private String tenant;
    private String entityId;
    private String entityType;
    private ProcessInstanceDTO processInstance;
    private String processCode;
    private String processType;
    private LocalDateTime createdOn;
    private String version;
    private String activeInd;
    private String completeInd;
    private String status;
    private boolean rmpSent;
    private String shipmentIndicator;
    private String approvedIndicator;
    private String directTruckingIndicator;
    private String phaseNumber;
    private int planNumber;
    private String flightSpecificIndicator;
    private String updateFlag;
    private Map<String, List<StepPlanDTO>> stepPlans;
}
