package com.ecommerce.ecommerce.product.service;

import com.ecommerce.ecommerce.common.exception.BusinessException;
import com.ecommerce.ecommerce.common.exception.ErrorCode;
import com.ecommerce.ecommerce.product.domain.Product;
import com.ecommerce.ecommerce.product.dto.ProductRequest;
import com.ecommerce.ecommerce.product.dto.ProductResponse;
import com.ecommerce.ecommerce.product.repository.ProductRepository;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ProductService {

    private final ProductRepository productRepository;

    @Transactional
    public ProductResponse create(ProductRequest request) {
        Product product = new Product(request.name(), request.price(), request.stock());
        Product saved = productRepository.save(product);
        return ProductResponse.toResponse(saved);
    }

    public ProductResponse findById(Long id) {
        return ProductResponse.toResponse(getProduct(id));
    }

    public List<ProductResponse> findAll() {
        return productRepository.findAll().stream()
                .map(ProductResponse::toResponse)
                .toList();
    }

    @Transactional
    public ProductResponse update(Long id, ProductRequest request) {
        Product product = getProduct(id);
        product.update(request.name(), request.price(), request.stock());
        return ProductResponse.toResponse(product);
    }

    @Transactional
    public void delete(Long id) {
        productRepository.delete(getProduct(id));
    }

    // TODO: 주문/결제 도메인에서 재고 차감 시 findByIdWithLock으로 비관적 락을 건 뒤 decreaseStock을 호출할 것
    @Transactional
    public void decreaseStock(Long id, int quantity) {
        Product product = productRepository.findByIdWithLock(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Product not found. id=" + id));
        product.decreaseStock(quantity);
    }

    private Product getProduct(Long id) {
        return productRepository.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "Product not found. id=" + id));
    }
}
