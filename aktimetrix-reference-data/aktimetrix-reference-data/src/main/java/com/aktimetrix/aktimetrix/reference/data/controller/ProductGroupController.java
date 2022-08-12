package com.aktimetrix.aktimetrix.reference.data.controller;

import com.aktimetrix.aktimetrix.reference.data.dto.ProductGroupDTO;
import com.aktimetrix.aktimetrix.reference.data.service.ProductGroupService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/encore/product-groups")
public class ProductGroupController {

    @Autowired
    private ProductGroupService productGroupService;

    @GetMapping
    public List<ProductGroupDTO> getAll() {
        return productGroupService.list();
    }
}
