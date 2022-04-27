package com.aktimetrix.service.processor.ciq.cdmpc.impl;

import com.aktimetrix.service.processor.core.api.MetadataProvider;
import com.aktimetrix.service.processor.core.transferobjects.Cargo;
import com.aktimetrix.service.processor.core.transferobjects.SpecialHandling;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@Component("processMetadataProvider")
@AllArgsConstructor
public class CiQProcessMetadataProvider implements MetadataProvider<Cargo> {

    /**
     * prepare metadata
     *
     * @return metadata
     */
    @Override
    public Map<String, Object> getMetadata(Cargo cargo) {

        Map<String, Object> metadata = new HashMap<>();
        metadata.put("origin", cargo.getOrigin().getCode());
        metadata.put("destination", cargo.getDestination().getCode());
        metadata.put("cargoType", cargo.getCargoType());
        metadata.put("cargoCategory", cargo.getCargoCategory());
        metadata.put("commodity", cargo.getCommodity());
        metadata.put("shipmentDescription", cargo.getDescription());
        metadata.put("jobReferenceNumber", cargo.getJobReferenceNumber());
        metadata.put("cargoReference", cargo.getCargoReference());
        metadata.put("eAWBIndicator", cargo.isEAWBIndicator());
        metadata.put("eFreightCode", "EAW");
        metadata.put("productCode", cargo.getProductCode());
        metadata.put("documentType", cargo.getDocumentInfo().getAwbInfo().getDocumentType());
        metadata.put("documentNumber", cargo.getDocumentInfo().getAwbInfo().getDocumentPrefix() + "-" + cargo.getDocumentInfo().getAwbInfo().getDocumentNumber());
        metadata.put("shcs", cargo.getShcList().stream().map(SpecialHandling::getCode).collect(Collectors.joining("-")));
        metadata.put("reservationPieces", cargo.getQuantityInfo().get(0).getPiece());
        metadata.put("reservationWeight", cargo.getQuantityInfo().get(0).getWeight().getValue());
        metadata.put("reservationWeightUnit", cargo.getQuantityInfo().get(0).getWeight().getUnit().getCode());
        metadata.put("reservationVolume", cargo.getQuantityInfo().get(0).getVolume().getValue());
        metadata.put("reservationVolumeUnit", cargo.getQuantityInfo().get(0).getVolume().getUnit().getCode());
        metadata.put("forwarderCode", cargo.getDocumentInfo().getAwbInfo().getParticipant()
                .stream().filter(p -> "AGT".equalsIgnoreCase(p.getType())).map(p -> p.getIdentifier()).collect(Collectors.joining()));

        return metadata;
    }
}
