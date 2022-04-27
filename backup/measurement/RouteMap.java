package com.aktimetrix.products.svm.ciq.cdmpc.model;

import lombok.Data;
import org.bson.types.ObjectId;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

/**
 * Its cdmp-c implementation of Plan.
 * <p>
 * Route Map is collection of Milestones
 *
 * @author arun kumar K
 */
@Data
@Document(collection = "routeMaps")
public class RouteMap {

    @Id
    private ObjectId id;
    private String tenant;
    // Unique plan identifier
    private ObjectId planId;
    private ObjectId processInstanceId;
    private String qualifiedInd;
    private String partnerInd;
    private String completeInd = "N";
    private String updatedInd;
    private String categoryInd;
    private String rmpSentId = "N";
    private String status;
    private String version;


    /**
     * Constructor
     *
     * @param tenant
     * @param planId
     * @param qualifiedInd
     * @param partnerInd
     * @param completeInd
     * @param updatedInd
     * @param categoryInd
     * @param rmpSentId
     * @param status
     * @param version
     */
    public RouteMap(String tenant, ObjectId planId, ObjectId processInstanceId, String qualifiedInd, String partnerInd, String completeInd, String updatedInd,
                    String categoryInd, String rmpSentId, String status, String version) {
        this.tenant = tenant;
        this.planId = planId;
        this.qualifiedInd = qualifiedInd;
        this.partnerInd = partnerInd;
        this.completeInd = completeInd;
        this.updatedInd = updatedInd;
        this.categoryInd = categoryInd;
        this.rmpSentId = rmpSentId;
        this.status = status;
        this.version = version;
        this.processInstanceId = processInstanceId;
    }
}
