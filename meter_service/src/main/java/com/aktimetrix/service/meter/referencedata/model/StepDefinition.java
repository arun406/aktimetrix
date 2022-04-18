package com.aktimetrix.service.meter.referencedata.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@ToString
@Document(collection = "stepDefinitions")
public class StepDefinition {

    private String tenant;
    @Id
    private String id;
    private String stepCode;
    private String stepName;
    private String optionalInd;
    private String categoryCode;
    private String subCategoryCode;
    private String status;
    private String functionalCtxCode;
    private String locationCtxCode;
    private String responsiblePartyCode;
    private List<String> startEventCodes;
    private List<String> endEventCodes;
    private String groupCode;
    private List<StepMeasurement> measurements;
}
