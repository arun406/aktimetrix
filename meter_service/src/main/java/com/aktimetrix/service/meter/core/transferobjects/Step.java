package com.aktimetrix.service.meter.core.transferobjects;

import lombok.Data;

import java.io.Serializable;
import java.util.Map;

@Data
public class Step implements Serializable {
    private String tenant;
    private String id;
    private String processCode;
    private String processInstanceId;
    private String categoryCode;
    private String subCategoryCode;
    private String responsiblePartyCode;
    private String code;
    private String name;
    private String locationCode;
    private String groupCode;
    private String status;
    private String functionalCtxCode;
    private String locationalCtxCode;
    private String createdOn;
    private int version;
    private int sequence;
    private Map<String, Object> metadata;

}
