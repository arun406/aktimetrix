package com.aktimetrix.service.meter.core.meter.generators;

import com.aktimetrix.service.meter.core.referencedata.encore.model.LineOffsetEncoreMaster;
import com.aktimetrix.service.meter.core.referencedata.encore.model.LinesEncoreMaster;
import com.aktimetrix.service.meter.core.referencedata.encore.service.FlightGroupEncoreMasterService;
import com.aktimetrix.service.meter.core.referencedata.encore.service.LinesEncoreMasterService;
import com.aktimetrix.service.meter.core.referencedata.encore.service.ProductGroupEncoreMasterService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 *
 */
@Component
@RequiredArgsConstructor
@Slf4j
public class OffsetCalculator {

    private final LinesEncoreMasterService linesEncoreMasterService;
    private final FlightGroupEncoreMasterService flightGroupEncoreMasterService;
    private final ProductGroupEncoreMasterService productGroupEncoreMasterService;

    /**
     * @param tenant   tenant key
     * @param metadata metadata
     * @param stepCode step code
     * @return offset value
     */
    public long getOffset(String tenant, Map<String, Object> metadata, String stepCode) {
        long offset = 0;
        final List<String> flightGroupCodes = getFlightGroups((String) metadata.get("flightNumber"));
        log.info(" Flight Groups: " + flightGroupCodes);

        final String productGroupCode = getProductGroupCode((String) metadata.get("productCode"));
        log.info(" Product Group Code: " + productGroupCode);

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate departureDate = LocalDate.parse((String) metadata.get("flightDate"), formatter);
        final String dow = String.valueOf(departureDate.getDayOfWeek().getValue());
        log.info(" dow : " + dow);

        final List<LinesEncoreMaster> lines = getLines(tenant, metadata, flightGroupCodes, productGroupCode, dow);
        if (lines != null && !lines.isEmpty()) {
            offset = getOffset(lines, stepCode);
        }
        return offset;
    }

    /**
     * @param lines    lines
     * @param stepCode step code
     * @return offset of the step
     */
    private long getOffset(List<LinesEncoreMaster> lines, String stepCode) {
        long offset = 0;
        final LinesEncoreMaster linesEncoreMaster = lines.get(0);

        final LineOffsetEncoreMaster lat = linesEncoreMaster.getOffsets().stream()
                .filter(lineOffsetEncoreMaster ->
                        lineOffsetEncoreMaster.getOffsetType().equalsIgnoreCase(stepCode))
                .collect(Collectors.toList())
                .stream().findFirst()
                .orElse(null);
        if (lat != null) {
            offset = Long.parseLong(lat.getOffsetValue());
            log.info(" Step Offset: " + offset);
        }
        return offset;
    }

    /**
     * @param tenant           tenant key
     * @param metadata         metadata
     * @param flightGroupCodes flight group codes
     * @param productCode      product group code
     * @param dow              day of working
     * @return list of encore lines
     */
    private List<LinesEncoreMaster> getLines(String tenant, Map<String, Object> metadata, List<String> flightGroupCodes,
                                             String productCode, String dow) {
        return linesEncoreMasterService.getLines(tenant,
                (String) metadata.get("forwarderCode"),
                (String) metadata.get("origin"),
                (String) metadata.get("acCategory"),
                (String) metadata.get("eFreightCode"),
                (String) metadata.get("flightNumber"),
                flightGroupCodes,
                (String) metadata.get("productCode"),
                productCode,
                "Y", null, null,
                dow);
    }


    /**
     * @param productCode product code
     * @return product group code
     */
    private String getProductGroupCode(String productCode) {
        final String groupCode = this.productGroupEncoreMasterService.getProductGroupCode(productCode);
        log.info(" product code : {}, group code : {}", productCode, groupCode);
        return groupCode;
    }


    /**
     * @param flightNumber flight number
     * @return Flight Groups
     */
    private List<String> getFlightGroups(String flightNumber) {
        final List<String> flightGroupCodes = this.flightGroupEncoreMasterService
                .getFlightGroupCodes(flightNumber);
        log.info(" flightGroupCodes : " + flightGroupCodes);
        return flightGroupCodes;
    }
}
