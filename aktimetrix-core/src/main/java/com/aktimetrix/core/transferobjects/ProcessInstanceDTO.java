package com.aktimetrix.core.transferobjects;

import lombok.Builder;
import lombok.Data;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ProcessInstanceDTO implements Serializable {
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
    private Map<String, Object> metadata = new HashMap<>();
    private List<StepInstanceDTO> steps = new ArrayList<>();
}
