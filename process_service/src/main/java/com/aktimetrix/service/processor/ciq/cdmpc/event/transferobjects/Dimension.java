package com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects;

import lombok.Data;

import java.util.List;

@Data
public class Dimension {
    public int serialNumber;
    public int piece;
    public UnitValue weightPerPiece;
    public UnitValue weight;
    public UnitValue length;
    public UnitValue width;
    public UnitValue height;
    public UnitValue volume;
    public List<SpecialHandling> specialHandling;
    public boolean tiltable;
    public boolean stackable;
}
