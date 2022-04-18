package com.aktimetrix.products.svm.ciq.cdmpc.measurement.util;

import com.aktimetrix.products.svm.ciq.cdmpc.measurement.LATPlanTimeGenerator;
import com.aktimetrix.products.svm.ciq.cdmpc.referencedata.encore.model.LineOffsetEncoreMaster;
import com.aktimetrix.products.svm.ciq.cdmpc.referencedata.encore.model.LinesEncoreMaster;
import com.aktimetrix.products.svm.ciq.cdmpc.referencedata.encore.service.FlightGroupEncoreMasterService;
import com.aktimetrix.products.svm.ciq.cdmpc.referencedata.encore.service.LinesEncoreMasterService;
import com.aktimetrix.products.svm.ciq.cdmpc.referencedata.encore.service.ProductGroupEncoreMasterService;
import com.aktimetrix.products.svm.core.model.StepInstance;
import org.bson.Document;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.stream.Collectors;

/**
 *
 */
@Component
public class OffsetCalculator {

    private static final Logger logger = LoggerFactory.getLogger(LATPlanTimeGenerator.class);

    private final LinesEncoreMasterService linesEncoreMasterService;
    private final FlightGroupEncoreMasterService flightGroupEncoreMasterService;
    private final ProductGroupEncoreMasterService productGroupEncoreMasterService;

    /**
     * @param linesEncoreMasterService
     * @param flightGroupEncoreMasterService
     * @param productGroupEncoreMasterService
     */
    public OffsetCalculator(LinesEncoreMasterService linesEncoreMasterService,
                            FlightGroupEncoreMasterService flightGroupEncoreMasterService,
                            ProductGroupEncoreMasterService productGroupEncoreMasterService) {
        this.linesEncoreMasterService = linesEncoreMasterService;
        this.flightGroupEncoreMasterService = flightGroupEncoreMasterService;
        this.productGroupEncoreMasterService = productGroupEncoreMasterService;
    }

    /**
     * @param tenant
     * @param bkdStepInstance
     * @param metadata
     * @param stepCode
     * @return
     */
    public long getOffset(String tenant, StepInstance bkdStepInstance, Document metadata, String stepCode) {
        long offset = 0;
        final List<String> flightGroupCodes = getFlightGroups(bkdStepInstance);
        logger.info(" Flight Groups: " + flightGroupCodes);

        final String productGroupCode = getProductGroupCode(metadata);
        logger.info(" Product Group Code: " + productGroupCode);

        final String flightDate = (String) bkdStepInstance.getMetadata().get("flightDate");
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
        LocalDate departureDate = LocalDate.parse(flightDate, formatter);
        final String dow = String.valueOf(departureDate.getDayOfWeek().getValue());
        logger.info(" dow : " + dow);

        final List<LinesEncoreMaster> lines = getLines(tenant, metadata, flightGroupCodes, productGroupCode, dow);
        if (lines != null && !lines.isEmpty()) {
            offset = getOffset(lines, stepCode);
        }
        return offset;
    }

    /**
     * @param lines
     * @param stepCode
     * @return
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
            logger.info(" Step Offset: " + offset);
        }
        return offset;
    }

    /**
     * @param tenant
     * @param metadata
     * @param flightGroupCodes
     * @param productCode
     * @param dow
     * @return
     */
    private List<LinesEncoreMaster> getLines(String tenant, Document metadata, List<String> flightGroupCodes,
                                             String productCode, String dow) {
        final List<LinesEncoreMaster> lines = linesEncoreMasterService.getLines(tenant,
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
        return lines;
    }

    /**
     * @param metadata
     * @return
     */
    private String getProductGroupCode(Document metadata) {
        final String productCode = this.productGroupEncoreMasterService.getProductGroupCode((String) metadata.get("productCode"));
        logger.info(" productCode : " + productCode);
        return productCode;
    }

    /**
     * @param bkdStepInstance
     * @return
     */
    private List<String> getFlightGroups(StepInstance bkdStepInstance) {
        final List<String> flightGroupCodes = this.flightGroupEncoreMasterService
                .getFlightGroupCodes((String) bkdStepInstance.getMetadata().get("flightNumber"));
        logger.info(" flightGroupCodes : " + flightGroupCodes);
        return flightGroupCodes;
    }
}
