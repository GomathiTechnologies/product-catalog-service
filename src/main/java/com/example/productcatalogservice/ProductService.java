package com.example.productcatalogservice;
import java.math.BigDecimal;
import java.util.List;
import org.springframework.stereotype.Service;
@Service
public class ProductService {
    public List<Product> getProducts() {
        return List.of(new Product("internet-500", "Internet 1gb", "High-speed home internet", 500, new BigDecimal("59.99"), "CAD"));
    }
}
