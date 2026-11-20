package com.example.productcatalogservice;
import java.math.BigDecimal;
public record Product(String id, String name, String description, int downloadSpeedMbps, BigDecimal monthlyPrice, String currency) {}
