package com.aktimetrix.service.processor.core.transferobjects;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Map;

@Data
public class Step {

    private String tenant;
    private String id;
    private String code;
    private String name;
    private String locationCode;
    private String groupCode;
    private String responsiblePartyCode;
    private String categoryCode;
    private String subCategoryCode;
    private String status;
    private int sequence;
    private int version;
    private String functionalCtxCode;
    private String locationalCtxCode;
    private Map<String, Object> metadata;

    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;
}
