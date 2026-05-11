package com.example.ecommerce.backend.order.controller;

import com.example.ecommerce.backend.common.constants.ApiEndpoints;
import com.example.ecommerce.backend.common.dto.response.ApiResponse;
import com.example.ecommerce.backend.order.dto.request.CreateOrderRequest;
import com.example.ecommerce.backend.order.dto.response.OrderCheckoutResponse;
import com.example.ecommerce.backend.order.dto.response.OrderResponse;
import com.example.ecommerce.backend.order.service.OrderService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.ExampleObject;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller for customer order checkout and lifecycle operations.
 *
 * <p>Exposes versioned order endpoints under
 * {@link ApiEndpoints.Order#BASE_ORDER} and wraps successful responses with the
 * common {@link ApiResponse} structure used by the API.</p>
 *
 * <p>The user identifier is temporarily hard-coded until authentication and a
 * user entity are introduced.</p>
 *
 * @author Pial Kanti Samadder
 */
@RestController
@RequestMapping(ApiEndpoints.Order.BASE_ORDER)
@RequiredArgsConstructor
@Tag(
        name = "Order",
        description = "Operations for checkout, order lookup, and cancellation"
)
public class OrderController {
    private static final Long CURRENT_USER_ID = 1L;

    private final OrderService orderService;

    /**
     * Checks out the current user's cart into a confirmed order and creates the
     * Stripe payment session.
     *
     * @param request validated checkout payload
     * @return response containing the confirmed order and Stripe payment link
     */
    @Operation(
            summary = "Checkout cart",
            description = "Creates a confirmed order from the current user's cart, reserves inventory, clears the cart, and returns a Stripe hosted Checkout URL for payment.",
            requestBody = @io.swagger.v3.oas.annotations.parameters.RequestBody(
                    required = true,
                    description = "Checkout payload containing the cart identifier to confirm.",
                    content = @Content(
                            mediaType = MediaType.APPLICATION_JSON_VALUE,
                            schema = @Schema(implementation = CreateOrderRequest.class),
                            examples = @ExampleObject(
                                    name = "Checkout cart",
                                    value = """
                                            {
                                              "cartId": 1
                                            }
                                            """
                            )
                    )
            ),
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Order confirmed successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = OrderCheckoutResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "400",
                            description = "Invalid checkout payload",
                            content = @Content
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "Cart or inventory does not exist",
                            content = @Content
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Cart is empty, product is inactive, stock is insufficient, cart belongs to another user, payment cannot be initiated, or coupon code is invalid",
                            content = @Content
                    )
            }
    )
    @PostMapping("/checkout")
    public ResponseEntity<ApiResponse<OrderCheckoutResponse>> checkout(
            @Valid @RequestBody CreateOrderRequest request) {
        return ResponseEntity.ok(ApiResponse.success(orderService.placeOrder(CURRENT_USER_ID, request)));
    }

    /**
     * Cancels a confirmed order and releases its reserved inventory.
     *
     * @param id order identifier
     * @return response containing the cancelled order
     */
    @Operation(
            summary = "Cancel order",
            description = "Cancels a confirmed order and releases all reserved inventory for its items.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Order cancelled successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = OrderResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "No order exists for the supplied identifier",
                            content = @Content
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Order does not belong to current user or is not confirmed",
                            content = @Content
                    )
            }
    )
    @PostMapping("/{id}/cancel")
    public ResponseEntity<ApiResponse<OrderResponse>> cancelOrder(
            @Parameter(description = "Unique identifier of the order to cancel.", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.cancelOrder(CURRENT_USER_ID, id)));
    }

    /**
     * Retrieves an order by identifier.
     *
     * @param id order identifier
     * @return response containing the matching order
     */
    @Operation(
            summary = "Get order by ID",
            description = "Retrieves one order with its item snapshots for the current user.",
            responses = {
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "200",
                            description = "Order retrieved successfully",
                            content = @Content(
                                    mediaType = MediaType.APPLICATION_JSON_VALUE,
                                    schema = @Schema(implementation = OrderResponse.class)
                            )
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "404",
                            description = "No order exists for the supplied identifier",
                            content = @Content
                    ),
                    @io.swagger.v3.oas.annotations.responses.ApiResponse(
                            responseCode = "409",
                            description = "Order does not belong to current user",
                            content = @Content
                    )
            }
    )
    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<OrderResponse>> getOrder(
            @Parameter(description = "Unique identifier of the order.", example = "1", required = true)
            @PathVariable Long id) {
        return ResponseEntity.ok(ApiResponse.success(orderService.getOrder(CURRENT_USER_ID, id)));
    }
}
