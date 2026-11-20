package com.example.productcatalogservice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
public class ProvinceProductsController {
    private final ProvinceProductsTool tool;

    public ProvinceProductsController(ProvinceProductsTool tool) {
        this.tool = tool;
    }

    @GetMapping("/dynamic/province/products")
    public ProvinceProductsTool.ProvinceProducts products(@RequestParam String province) {
        try {
            return tool.getProvinceProducts(province);
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage(), exception);
        }
    }
}
