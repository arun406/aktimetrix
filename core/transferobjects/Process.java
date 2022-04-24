package com.aktimetrix.core.transferobjects;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
public class Process {

    private String tenant;
    private String id;
    private String entityId;
    private String entityType;
    private String processCode;
    private String categoryCode;
    private String subCategoryCode;
    private String status;
    private int version;
    private boolean active;
    private boolean valid;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm")
    private LocalDateTime createdOn;
    private Map<String, Object> metadata;
    private List<Step> steps = new ArrayList<>();

}
