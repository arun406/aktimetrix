package com.aktimetrix.service.processor.ciq.cdmpc.impl;

import com.aktimetrix.core.api.MetadataProvider;
import com.aktimetrix.service.processor.ciq.cdmpc.event.transferobjects.Itinerary;
import lombok.AllArgsConstructor;
import org.apache.commons.lang3.StringUtils;
import org.springframework.stereotype.Component;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;

@Component("stepMetadataProvider")
@AllArgsConstructor
public class CiQStepMetadataProvider implements MetadataProvider<Itinerary> {

    /**
     * prepare metadata
     *
     * @return metadata
     */
    @Override
    public Map<String, Object> getMetadata(Itinerary data) {
        Map<String, Object> metadata = new HashMap<>();
        metadata.put("boardPoint", data.getBoardPoint().getCode());
        metadata.put("offPoint", data.getOffPoint().getCode());
        String flightNumber = data.getTransportInfo().getCarrier()
                + data.getTransportInfo().getNumber();
        if (StringUtils.isNotEmpty(data.getTransportInfo().getExtensionNumber())) {
            flightNumber = flightNumber + data.getTransportInfo().getExtensionNumber();
        }
        metadata.put("flightNumber", flightNumber);
        metadata.put("flightDate", data.getDepartureDateTimeLocal().getEstimated().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")));
        metadata.put("std", data.getDepartureDateTimeLocal().getSchedule());
        metadata.put("etd", data.getDepartureDateTimeLocal().getEstimated());
        metadata.put("atd", data.getDepartureDateTimeLocal().getActual());
        metadata.put("sta", data.getArrivalDateTimeLocal().getSchedule());
        metadata.put("eta", data.getArrivalDateTimeLocal().getEstimated());
        metadata.put("ata", data.getArrivalDateTimeLocal().getActual());
        metadata.put("pieces", data.getQuantity().getPiece());
        metadata.put("wt", data.getQuantity().getWeight().getValue());
        metadata.put("wtUnit", data.getQuantity().getWeight().getUnit().getCode());
        metadata.put("volUnit", data.getQuantity().getVolume().getUnit().getCode());
        metadata.put("vol", data.getQuantity().getVolume().getValue());
        metadata.put("acCategory", data.getAircraftCategory());
        return metadata;
    }
}
