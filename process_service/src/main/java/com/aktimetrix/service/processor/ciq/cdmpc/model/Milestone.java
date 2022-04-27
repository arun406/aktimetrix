package com.aktimetrix.service.processor.ciq.cdmpc.model;

import com.aktimetrix.core.model.StepInstance;
import lombok.Data;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@Document(collection = "milestones")
public class Milestone extends StepInstance {
    private String completeIndicator;
    private String mupSentIndicator;
    private String sequenceNumber;

    public Milestone() {
        super();
    }
}
