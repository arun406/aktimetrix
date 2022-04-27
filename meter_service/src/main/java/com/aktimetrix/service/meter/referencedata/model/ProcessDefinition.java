package com.aktimetrix.service.meter.referencedata.model;

import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@ToString
@NoArgsConstructor
@Document(collection = "processDefinitions")
public class ProcessDefinition {
    private String tenant;
    @Id
    private String id;
    private String processCode;
    private String processName;
    private String processDescription;
    private String categoryCode;
    private String subCategoryCode;
    private List<String> tags;
    private String entityType;
    private List<String> startEventCodes;
    private List<String> endEventCodes;
    private String status;
    private String responsiblePartyCode;
    private String groupCode;
    private List<StepDefinition> steps;
    private List<String> measurements;

    /**
     * @param tenant
     * @param processCode
     */
    public ProcessDefinition(String tenant, String processCode) {
        this.tenant = tenant;
        this.processCode = processCode;
    }
}
