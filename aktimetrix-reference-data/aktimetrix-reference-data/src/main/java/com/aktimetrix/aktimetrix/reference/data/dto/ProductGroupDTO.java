package com.aktimetrix.aktimetrix.reference.data.dto;

import lombok.Data;
import lombok.ToString;

@Data
@ToString
public class ProductGroupDTO {
    private String airline;
    private String productCode;
    private String productName;
    private String productGroupCode;
    private String productGroupName;

    /**
     * constructor
     *
     * @param airline
     * @param productCode
     * @param productName
     * @param productGroupCode
     * @param productGroupName
     */
    public ProductGroupDTO(String airline, String productCode, String productName, String productGroupCode, String productGroupName) {
        this.airline = airline;
        this.productCode = productCode;
        this.productName = productName;
        this.productGroupCode = productGroupCode;
        this.productGroupName = productGroupName;
    }
}
