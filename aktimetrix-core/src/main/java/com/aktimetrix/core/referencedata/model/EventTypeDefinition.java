package com.aktimetrix.core.referencedata.model;

import lombok.Data;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.util.List;

@Data
@Document(collection = "eventTypeDefinitions")
public class EventTypeDefinition {
    private String tenant;
    @Id
    private String id;
    private String eventType;
    private String eventCode;
    private String eventName;
    private String eventCategory; // planning , monitoring
    private String eventDescription;
    private String eventSource; // SC
    private List<String> contentTypes; // application-json
}
