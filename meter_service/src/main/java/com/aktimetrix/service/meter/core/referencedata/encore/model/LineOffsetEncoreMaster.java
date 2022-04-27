package com.aktimetrix.service.meter.core.referencedata.encore.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "lineOffset")
public class LineOffsetEncoreMaster {

    private String tenant;
    private ObjectId id;
    private String offsetType;
    private String offsetValue;
}
