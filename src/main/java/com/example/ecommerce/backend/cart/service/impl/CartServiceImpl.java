package com.example.ecommerce.backend.cart.service.impl;

import com.example.ecommerce.backend.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.backend.cart.dto.response.CartResponse;
import com.example.ecommerce.backend.cart.dto.response.CartSuggestionResponse;
import com.example.ecommerce.backend.cart.entity.Cart;
import com.example.ecommerce.backend.cart.entity.CartItem;
import com.example.ecommerce.backend.cart.mapper.CartMapper;
import com.example.ecommerce.backend.cart.repository.CartRepository;
import com.example.ecommerce.backend.cart.service.CartService;
import com.example.ecommerce.backend.common.exception.ResourceConflictException;
import com.example.ecommerce.backend.common.utils.StringSimilarityUtil;
import com.example.ecommerce.backend.inventory.entity.Inventory;
import com.example.ecommerce.backend.inventory.repository.InventoryRepository;
import com.example.ecommerce.backend.product.entity.Product;
import com.example.ecommerce.backend.product.mapper.ProductMapper;
import com.example.ecommerce.backend.product.repository.ProductRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * Implementation of {@link CartService} for managing user shopping carts.
 *
 * <p>Handles cart lookup, first-cart creation, product validation, inventory
 * availability checks, item quantity merging, persistence, and response
 * mapping. Inventory is checked before cart mutation, but it is not reserved by
 * cart operations.</p>
 *
 * @author Pial Kanti Samadder
 */
@Service
@RequiredArgsConstructor
public class CartServiceImpl implements CartService {
    private final CartRepository cartRepository;
    private final ProductRepository productRepository;
    private final InventoryRepository inventoryRepository;
    private final CartMapper cartMapper;
    private final ProductMapper productMapper;

    @Override
    @Transactional
    public CartResponse addItem(Long userId, CartItemAddRequest request) {
        Product product = productRepository.findById(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Product not found: " + request.productId()));

        if (!Boolean.TRUE.equals(product.getIsActive())) {
            throw new ResourceConflictException("Inactive product cannot be added to cart: " + request.productId());
        }

        Inventory inventory = inventoryRepository.findByProductId(request.productId())
                .orElseThrow(() -> new EntityNotFoundException("Inventory not found for product: " + request.productId()));

        Cart cart = cartRepository.findByUserId(userId)
                .orElseGet(() -> createCart(userId));

        Optional<CartItem> existingItem = findItemByProductId(cart, request.productId());
        int requestedCartQuantity = request.quantity() + existingItem
                .map(CartItem::getQuantity)
                .orElse(0);

        validateAvailableQuantity(inventory, requestedCartQuantity);

        if (existingItem.isPresent()) {
            CartItem cartItem = existingItem.get();
            cartItem.setQuantity(requestedCartQuantity);
        } else {
            cart.getItems().add(CartItem.builder()
                    .cart(cart)
                    .product(product)
                    .quantity(request.quantity())
                    .unitPrice(product.getPrice())
                    .build());
        }

        return cartMapper.toResponse(cartRepository.saveAndFlush(cart));
    }

    @Override
    @Transactional(readOnly = true)
    public CartResponse getCart(Long userId) {
        return cartRepository.findByUserId(userId)
                .map(cartMapper::toResponse)
                .orElseGet(() -> cartMapper.toEmptyResponse(userId));
    }

    @Override
    @Transactional
    public void clearCart(Long userId, Long cartId) {
        Cart cart = cartRepository.findById(cartId)
                .orElseThrow(() -> new EntityNotFoundException("Cart not found: " + cartId));

        if (!cart.getUserId().equals(userId)) {
            throw new ResourceConflictException("Cart does not belong to current user: " + cartId);
        }

        cart.getItems().clear();
        cartRepository.saveAndFlush(cart);
    }

    @Override
    @Transactional(readOnly = true)
    public CartSuggestionResponse getCartSuggestions(Long userId) {
        Cart cart = cartRepository.findByUserId(userId).orElse(null);

        // If no cart exists or cart is empty, return empty suggestions
        if (cart == null || cart.getItems().isEmpty()) {
            return new CartSuggestionResponse(userId, List.of());
        }

        // Get product IDs already in the cart
        Set<Long> cartProductIds = new HashSet<>();
        for (CartItem item : cart.getItems()) {
            cartProductIds.add(item.getProduct().getId());
        }

        // Sort cart items by product price in ascending order
        List<CartItem> sortedItems = cart.getItems().stream()
                .sorted(Comparator.comparingDouble(item -> item.getProduct().getPrice()))
                .toList();

        List<Product> suggestions = new ArrayList<>();

        // For each cart item, find up to 1 related product
        for (CartItem cartItem : sortedItems) {
            if (suggestions.size() >= 3) {
                break; // Stop when we have 3 suggestions
            }

            Product cartProduct = cartItem.getProduct();
            Product relatedProduct = findMostSimilarProduct(cartProduct, cartProductIds);

            if (relatedProduct != null) {
                suggestions.add(relatedProduct);
                cartProductIds.add(relatedProduct.getId()); // Prevent duplicates
            }
        }

        return new CartSuggestionResponse(userId, suggestions.stream()
                .map(productMapper::toResponse)
                .toList());
    }

    /**
     * Finds the most similar product to the given product.
     *
     * <p>Matching criteria:
     * 1. Same category
     * 2. Price difference <= 100
     * 3. Highest name similarity (lowest Levenshtein distance)</p>
     *
     * @param product the product to find similar products for
     * @param excludeProductIds product IDs to exclude from the search (e.g., already in cart)
     * @return the most similar product, or null if no match found
     */
    private Product findMostSimilarProduct(Product product, Set<Long> excludeProductIds) {
        // Get all products in the same category
        List<Product> categoryProducts = productRepository.findAll().stream()
                .filter(p -> p.getCategory().getId().equals(product.getCategory().getId())
                        && !p.getId().equals(product.getId())
                        && !excludeProductIds.contains(p.getId())
                        && Boolean.TRUE.equals(p.getIsActive()))
                .toList();

        Product bestMatch = null;
        int bestDistance = Integer.MAX_VALUE;

        for (Product candidate : categoryProducts) {
            // Check if price difference is within 100
            double priceDifference = Math.abs(product.getPrice() - candidate.getPrice());
            if (priceDifference > 100) {
                continue;
            }

            // Calculate Levenshtein distance for product names
            int distance = StringSimilarityUtil.levenshteinDistance(product.getName(), candidate.getName());

            // Track the product with the lowest distance (highest similarity)
            if (distance < bestDistance) {
                bestDistance = distance;
                bestMatch = candidate;
            }
        }

        return bestMatch;
    }

    private Cart createCart(Long userId) {
        return Cart.builder()
                .userId(userId)
                .createdBy(userId)
                .modifiedBy(userId)
                .build();
    }

    private Optional<CartItem> findItemByProductId(Cart cart, Long productId) {
        return cart.getItems()
                .stream()
                .filter(item -> item.getProduct().getId().equals(productId))
                .findFirst();
    }

    private void validateAvailableQuantity(Inventory inventory, int requestedCartQuantity) {
        int availableQuantity = inventory.getTotalQuantity() - inventory.getReservedQuantity();
        if (requestedCartQuantity > availableQuantity) {
            throw new ResourceConflictException("Insufficient available inventory for product: "
                                                + inventory.getProduct().getId());
        }
    }
}
