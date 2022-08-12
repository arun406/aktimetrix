package com.aktimetrix.aktimetrix.reference.data.model;

import lombok.Data;
import lombok.ToString;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Data
@ToString
@Document(collection = "productGroups")
public class ProductGroup {

    @Id
    private String id;
    private String airline;
    private String productCode;
    private String productName;
    private String productGroup;
    private String productGroupName;

    /**
     * constructor
     *
     * @param airline
     * @param productCode
     * @param productName
     * @param productGroup
     * @param productGroupName
     */
    public ProductGroup(String airline, String productCode, String productName, String productGroup, String productGroupName) {
        this.airline = airline;
        this.productCode = productCode;
        this.productName = productName;
        this.productGroup = productGroup;
        this.productGroupName = productGroupName;
    }
}
