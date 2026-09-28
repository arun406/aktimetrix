package com.aktimetrix.core.model;

import com.aktimetrix.core.api.Timeliness;
import com.aktimetrix.core.referencedata.model.ProcessDefinition;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.ser.std.ToStringSerializer;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.annotation.Transient;
import org.springframework.data.annotation.Version;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
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
    /**
     * When the process started in the business: the time of the event that started it.
     */
    private LocalDateTime startedAt;
    private Map<String, Object> metadata;
    /**
     * When the whole process should complete: its start plus the definition's {@code plannedWithin}, if any.
     */
    private LocalDateTime plannedAt;
    /**
     * When the process counts as late: {@code plannedAt} plus the definition's tolerance.
     */
    private LocalDateTime lateAfter;
    /**
     * When the process completed or was cancelled: the time of the event that ended it.
     */
    private LocalDateTime endedAt;
    /**
     * How the process compares with its deadline; {@code null} when it has none, or until it can be judged.
     */
    private Timeliness timeliness;
    /**
     * Incremented on every save; a save based on a stale copy fails instead of overwriting a newer state.
     */
    @Version
    @JsonIgnore
    private Long revision;
    @Transient
    private List<StepInstance> steps = new ArrayList<>();

    /**
     * @param definition process definition
     */
    public ProcessInstance(ProcessDefinition definition) {
        this.processCode = definition.getProcessCode();
        this.categoryCode = definition.getCategoryCode();
        this.subCategoryCode = definition.getSubCategoryCode();
        this.status = "Created";
        this.version = 1;
        this.createdOn = LocalDateTime.now();
        this.tenant = definition.getTenant();
        this.entityType = definition.getEntityType();
    }
}
