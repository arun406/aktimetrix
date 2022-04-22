package com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects;

import lombok.Data;

@Data
public class TransportInfo {
    public String carrier;
    public String number;
    public String extensionNumber;
}
