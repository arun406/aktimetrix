package com.aktimetrix.service.processor.core.model;

import lombok.Data;
import org.bson.Document;
import org.bson.types.ObjectId;

import java.time.Instant;

@Data
@org.springframework.data.mongodb.core.mapping.Document(collection = "events")
public class Event {

    private String tenant;
    private ObjectId id;
    private String eventCode;
    private String entityId;
    private String entityType;
    private Document eventDetails;
    private Instant createDateTime;
    private Instant modifiedDateTime;
    private String createdBy;
    private String modifiedBy;
    private String status;

}
