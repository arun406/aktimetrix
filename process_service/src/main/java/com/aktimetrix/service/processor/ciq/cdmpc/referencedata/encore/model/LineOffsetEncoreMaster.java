package com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.model;

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
