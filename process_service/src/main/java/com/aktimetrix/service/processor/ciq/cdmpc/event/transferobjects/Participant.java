package com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects;

import lombok.Data;

@Data
public class Participant {
    public String type;
    public String firstName;
    public String name;
    public String identifier;
}
