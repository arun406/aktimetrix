package com.aktimetrix.core.transferobjects;

import lombok.Data;

import java.util.List;

@Data
public class QuantityInfo {
    private int piece;
    private Object slac;
    private UnitValue weight;
    private UnitValue volume;
    private List<Dimension> dimension;
    private List<UldDetail> uldDetails;
    private List<SpecialHandling> shcList;
}
