package com.aktimetrix.core.transferobjects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.aktimetrix.core.api.Timeliness;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ProcessInstanceDTO implements Serializable {
    private String id;
    private String entityId;
    private String entityType;
    private String tenant;
    private String processCode;
    private String categoryCode;
    private String subCategoryCode;
    private String status;
    private int version;
    private boolean active;
    private boolean valid;
    private boolean complete;
    private Map<String, Object> metadata;
    private LocalDateTime startedAt;
    private LocalDateTime plannedAt;
    private LocalDateTime lateAfter;
    private LocalDateTime endedAt;
    private Timeliness timeliness;
    private List<StepInstanceDTO> steps;
}
