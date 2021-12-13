package kitchenpos.application.product;

import kitchenpos.domain.Price;
import kitchenpos.domain.product.Product;
import kitchenpos.domain.product.ProductRepository;
import kitchenpos.dto.menu.MenuProductDto;
import kitchenpos.dto.product.ProductDto;
import kitchenpos.exception.product.NotFoundProductException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductService {
    private final ProductRepository productRepository;

    public ProductService(final ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Transactional
    public ProductDto create(final ProductDto productDto) {
        Product product = Product.of(productDto.getName(), Price.of(productDto.getPrice()));

        return ProductDto.of(productRepository.save(product));
    }

    public List<ProductDto> list() {
        return productRepository.findAll().stream()
                                .map(ProductDto::of)
                                .collect(Collectors.toList());
    }

    public Product findById(Long productId) {
        return productRepository.findById(productId).orElseThrow(NotFoundProductException::new);
    }

    public Price sumOfPrices(final List<MenuProductDto> menuProducts) {
        Price sum = Price.of(0);

        for (final MenuProductDto menuProduct : menuProducts) {
            final Product product = productRepository.findById(menuProduct.getProductId()).orElseThrow(NotFoundProductException::new);

            sum = sum.add(product.getPrice().multiply(menuProduct.getQuantity()));
        }

        return sum;
    }
}
