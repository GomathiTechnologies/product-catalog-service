package com.example.productcatalogservice;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/cart")
public class CartController {
    private final CartTool cartTool;

    public CartController(CartTool cartTool) {
        this.cartTool = cartTool;
    }

    @PostMapping("/add")
    public CartTool.SampleOrder addToCart(@RequestBody AddToCartRequest request) {
        try {
            return cartTool.addToCart(request.productId());
        } catch (IllegalArgumentException exception) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, exception.getMessage());
        }
    }

    public record AddToCartRequest(String productId) {}
}
