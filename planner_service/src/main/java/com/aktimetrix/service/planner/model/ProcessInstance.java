package com.aktimetrix.service.planner.model;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 *
 */
@Data
@Document(collection = "process-instance")
@AllArgsConstructor
@NoArgsConstructor
public class ProcessInstance {

    @Id
    @JsonSerialize(using = ToStringSerializer.class)
    private ObjectId id;
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
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdOn;
    private Map<String, Object> metadata;
    @Transient
    private List<StepInstance> steps = new ArrayList<>();
}
