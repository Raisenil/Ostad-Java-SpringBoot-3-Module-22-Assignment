package com.example.ecommerce.backend.cart.dto.response;

import com.example.ecommerce.backend.product.dto.response.ProductResponse;

import java.util.List;

/**
 * Response payload containing cart product suggestions.
 *
 * <p>Returns up to 3 related products that complement the user's current cart items.
 * Suggestions are based on category similarity and price proximity.</p>
 *
 * @author Pial Kanti Samadder
 */
public record CartSuggestionResponse(
        Long userId,
        List<ProductResponse> suggestions
) {
}

