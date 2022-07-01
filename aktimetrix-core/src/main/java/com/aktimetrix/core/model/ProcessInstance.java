package com.aktimetrix.core.model;

import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Data
@Document(collection = "processInstances")
@AllArgsConstructor
@NoArgsConstructor
public class ProcessInstance {

    @Id
    private String id;
    private String entityId;
    private String entityType;
    private String tenant;
    private String processCode;
    private String processType;
    private String categoryCode;
    private String subCategoryCode;
    private String status;
    private int version;
    private boolean active;
    private boolean valid;
    private boolean complete;
    private String approvedIndicator;
    private String shipmentIdentifier;
    private LocalDateTime createdOn;
    private String createdBy;
    private LocalDateTime modifiedOn;
    private String modifiedBy;
    private Map<String, Object> metadata = new HashMap<>();
    private List<StepInstance> steps = new ArrayList<>();
    private String cancellationReason;

    /**
     * @param definition process definition
     */
    public ProcessInstance(ProcessDefinition definition) {
        this.processCode = definition.getProcessCode();
        this.processType = definition.getProcessType();
        this.categoryCode = definition.getCategoryCode();
        this.subCategoryCode = definition.getSubCategoryCode();
        this.status = "Created";
        this.version = 1;
        this.approvedIndicator = "A";
        this.createdOn = LocalDateTime.now();
        this.tenant = definition.getTenant();
        this.entityType = definition.getEntityType();
        this.setSteps(new ArrayList<>());
    }
}
