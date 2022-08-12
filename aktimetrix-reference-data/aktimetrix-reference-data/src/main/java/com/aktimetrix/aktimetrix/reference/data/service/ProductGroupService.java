package com.aktimetrix.aktimetrix.reference.data.service;

import com.aktimetrix.aktimetrix.reference.data.dto.ProductGroupDTO;
import com.aktimetrix.aktimetrix.reference.data.model.ProductGroup;
import com.aktimetrix.aktimetrix.reference.data.repository.ProductGroupRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ProductGroupService {

    private final ProductGroupRepository repository;

    /**
     * saves the list of product groups
     *
     * @param productGroupDTOs
     */
    public void save(List<ProductGroupDTO> productGroupDTOs) {
        List<ProductGroup> productGroups = productGroupDTOs.stream().map(dto ->
                        new ProductGroup(dto.getAirline(), dto.getProductCode(), dto.getProductGroupCode(), dto.getProductGroupCode(), dto.getProductGroupName()))
                .collect(Collectors.toList());

        this.repository.saveAll(productGroups);
    }

    /**
     * save the list of product group
     *
     * @param dto
     */
    public void save(ProductGroupDTO dto) {
        ProductGroup productGroup = new ProductGroup(dto.getAirline(), dto.getProductCode(), dto.getProductGroupCode(),
                dto.getProductGroupCode(), dto.getProductGroupName());
        this.repository.save(productGroup);
    }


    /**
     * list all records without pagination
     *
     * @return
     */
    public List<ProductGroupDTO> list() {
        List<ProductGroup> productGroups = this.repository.findAll();
        if (productGroups != null) {
            return productGroups.stream()
                    .map(model -> new ProductGroupDTO(model.getAirline(), model.getProductCode(), model.getProductName(), model.getProductGroup(), model.getProductGroupName()))
                    .collect(Collectors.toList());
        }
        return new ArrayList<>();
    }

    /**
     *
     */
    public void cleanup() {
        this.repository.deleteAll();
    }
}
