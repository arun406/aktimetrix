package com.aktimetrix.service.processor.ciq.cdmpc.service;

import com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.model.RouteEncoreMaster;
import com.aktimetrix.service.processor.ciq.cdmpc.referencedata.encore.service.RouteEncoreMasterService;
import com.aktimetrix.service.processor.core.model.StepInstance;
import com.aktimetrix.service.processor.core.referencedata.tenant.service.TenantService;
import com.aktimetrix.service.processor.core.transferobjects.Cargo;
import com.aktimetrix.service.processor.core.transferobjects.Itinerary;
import com.aktimetrix.service.processor.core.transferobjects.SpecialHandling;
import org.apache.commons.lang3.StringUtils;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

@Service
public class CDMPCCommonUtil {

    private static final Logger logger = LoggerFactory.getLogger(CDMPCCommonUtil.class);
    @Autowired
    TenantService tenantService;

    @Autowired
    RouteEncoreMasterService routeEncoreMasterService;

    /**
     * @param data
     * @return
     */
    public Document getStepInstanceMetadata(Itinerary data) {
        Document metadata = new Document();
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

    /**
     * returns true if shipment is partner shipment else false
     *
     * @param forwarderCode
     * @return
     */
    public boolean isPartnerShipment(String tenant, String airline, String forwarderCode, String origin, String destination) {

        String value = this.tenantService.getValue(tenant, "SVM", "CIQ_NETWORK_FORWARDER");
        logger.info("Value of CIQ_NETWORK_FORWARDER property :  " + value);
        if (value != null && value.equalsIgnoreCase(forwarderCode)) {
            return true;
        } else {
            final RouteEncoreMaster route = this.routeEncoreMasterService
                    .getRouteByAirlineCodeAndOriginAndDestinationAndForwarderCode(tenant, airline, origin, destination, forwarderCode);
            logger.info("route : " + route);
            if (route != null) {
                return true;
            }
        }
        return false;
    }


    /**
     * Creates metadata map from cargo object
     *
     * @param cargo cargo
     * @return metadata
     */
    public Map<String, Object> getProcessInstanceMetadata(Cargo cargo) {
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


    /**
     * @param stepInstances
     * @return
     */
    public List<Document> getItinerariesFromProcessInstance(List<StepInstance> stepInstances) {

        List<Document> itineraries = null;
        // fetch all step instances for the give processInstanceId
        // get the bp and op pair from the step instances

        if (stepInstances != null && !stepInstances.isEmpty()) {
            AtomicInteger counter = new AtomicInteger(0);
            itineraries = stepInstances.stream()
                    .filter(si -> si.getStepCode().equalsIgnoreCase("BKD") && si.getMetadata() != null)
                    .map(si -> si.getMetadata())
                    .map(d -> {
                        Document document = new Document();
                        document.put("boardPoint", d.get("boardPoint"));
                        document.put("offPoint", d.get("offPoint"));
                        document.put("flightNumber", d.get("flightNumber"));
                        document.put("flightDate", d.get("flightDate"));
                        document.put("pieces", d.get("pieces"));
                        document.put("wt", d.get("wt"));
                        document.put("wtUnit", d.get("wtUnit"));
                        document.put("vol", d.get("vol"));
                        document.put("volUnit", d.get("volUnit"));
                        return document;
                    })
                    .distinct()
                    .collect(Collectors.toList());
        }
        return itineraries;
    }

}
