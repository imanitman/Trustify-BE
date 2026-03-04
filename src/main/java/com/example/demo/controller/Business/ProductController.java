package com.example.demo.controller.Business;

import com.trustify.trustify.dto.Req.ReqProductReview;
import com.trustify.trustify.entity.Company;
import com.trustify.trustify.entity.Product;
import com.trustify.trustify.service.Business.CompanyService;
import com.trustify.trustify.service.Business.ProductService;
import com.trustify.trustify.service.Customer.ReviewService;
import lombok.AllArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@AllArgsConstructor
@RequestMapping("api/v1/product")
public class ProductController {
    private final ProductService productService;
    private final CompanyService companyService;
    private final ReviewService reviewService;

    @GetMapping("/{productCode}/review")
    public ResponseEntity<?> getReviewByProductId(@PathVariable String productCode) {
        Product product = productService.getProductByProductCode(productCode);
        Long productId = product.getId();
        return ResponseEntity.ok(reviewService.getReviewsByProductId(productId));
    }

    @PostMapping("review/product")
    public ResponseEntity<?> writeReviewProduct(@RequestBody ReqProductReview reqProductReview) {
        Company company = companyService.getCompanyById(reqProductReview.getCompanyId());
        List<Product> products = company.getProducts();
        for (Product product : products) {
            if (product.getProductCode().equals(reqProductReview.getProductCode())) {
                reviewService.saveReviewProduct(reqProductReview.getReview(), product, reqProductReview.getEmail());
                return ResponseEntity.ok("Review saved successfully");
            }
        }
        return ResponseEntity.badRequest().body("Product not found");
    }
}
