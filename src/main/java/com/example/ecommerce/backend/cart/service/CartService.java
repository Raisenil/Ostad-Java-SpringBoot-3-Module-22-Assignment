package com.example.ecommerce.backend.cart.service;

import com.example.ecommerce.backend.cart.dto.request.CartItemAddRequest;
import com.example.ecommerce.backend.cart.dto.response.CartResponse;
import com.example.ecommerce.backend.cart.dto.response.CartSuggestionResponse;

/**
 * Service interface for shopping cart operations.
 *
 * <p>Provides the current cart view and product add behavior while keeping the
 * temporary current-user lookup outside the persistence layer.</p>
 *
 * @author Pial Kanti Samadder
 */
public interface CartService {
    /**
     * Adds a product item to the user's cart.
     *
     * @param userId owner user identifier
     * @param request cart item add payload
     * @return updated cart response
     * @throws jakarta.persistence.EntityNotFoundException when product or inventory is missing
     * @throws com.example.ecommerce.backend.common.exception.ResourceConflictException when product is inactive or stock is insufficient
     */
    CartResponse addItem(Long userId, CartItemAddRequest request);

    /**
     * Retrieves the user's current cart.
     *
     * @param userId owner user identifier
     * @return current cart response, or an empty cart response when no cart exists
     */
    CartResponse getCart(Long userId);

    /**
     * Removes all items from a confirmed cart.
     *
     * @param userId owner user identifier
     * @param cartId cart identifier
     * @throws jakarta.persistence.EntityNotFoundException when cart is missing
     * @throws com.example.ecommerce.backend.common.exception.ResourceConflictException when cart belongs to another user
     */
    void clearCart(Long userId, Long cartId);

    /**
     * Retrieves cart product suggestions for the user.
     *
     * <p>Returns up to 3 related products based on the user's current cart items.
     * Suggestions are found by matching products in the same category with similar prices
     * and names. Products already in the cart are excluded.</p>
     *
     * @param userId owner user identifier
     * @return suggestion response containing up to 3 related products
     */
    CartSuggestionResponse getCartSuggestions(Long userId);
}
