package com.example.demo.service.Business;

import com.trustify.trustify.entity.Product;
import com.trustify.trustify.repository.Business.ProductRepository;
import lombok.AllArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@AllArgsConstructor
public class ProductService {
    private final ProductRepository productRepository;

    public Product saveProduct(Product product) {
        return productRepository.save(product);
    }

    public Product getProductByProductCode(String productCode) {
        return productRepository.findByProductCode(productCode);
    }
}
