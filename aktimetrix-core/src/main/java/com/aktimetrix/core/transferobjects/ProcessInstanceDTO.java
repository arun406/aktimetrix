package com.aktimetrix.core.transferobjects;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import com.aktimetrix.core.api.Timeliness;

import java.io.Serializable;
import java.time.Instant;
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
    /**
     * Name of the process, from its definition.
     */
    private String processName;
    /**
     * Revision of the process definition the instance follows: the one it started with.
     */
    private Long definitionRevision;
    private String categoryCode;
    private String subCategoryCode;
    private String status;
    private int version;
    private boolean active;
    private boolean valid;
    private boolean complete;
    private Map<String, Object> metadata;
    private Instant startedAt;
    private Instant plannedAt;
    private Instant lateAfter;
    private Instant endedAt;
    private Timeliness timeliness;
    private int run;
    private List<StepInstanceDTO> steps;
}
