package com.example.ecommerce.backend.common.utils;

import com.example.ecommerce.backend.common.exception.ResourceConflictException;
import lombok.Getter;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Utility class for coupon code validation and discount calculation.
 *
 * <p>Provides discount calculation logic for valid coupon codes with
 * percentage-based discounts subject to maximum caps.</p>
 */
public class CouponUtil {

    private CouponUtil() {
        // Private constructor to prevent instantiation
    }

    /**
     * Coupon configuration with percentage and cap information.
     */
    @Getter
    private static final class CouponConfig {
        private final double percentage;
        private final double cap;

        CouponConfig(double percentage, double cap) {
            this.percentage = percentage;
            this.cap = cap;
        }
    }

    /**
     * Map of valid coupon codes to their discount configurations.
     */
    private static final Map<String, CouponConfig> VALID_COUPONS = new HashMap<>();

    static {
        VALID_COUPONS.put("DESH10", new CouponConfig(10.0, 100.0));
        VALID_COUPONS.put("NINE11", new CouponConfig(11.0, 200.0));
        VALID_COUPONS.put("MIND20", new CouponConfig(20.0, 1000.0));
    }

    /**
     * Calculates the discount amount for a given coupon code and order total.
     *
     * <p>The discount is computed as (totalAmount * percentage / 100) capped
     * at the coupon's maximum discount amount.</p>
     *
     * @param couponCode the coupon code to validate and apply
     * @param totalAmount the order total amount before discount
     * @return the discount amount to apply
     * @throws ResourceConflictException if the coupon code is invalid
     */
    public static double calculateDiscount(String couponCode, Double totalAmount) {
        if (couponCode == null || couponCode.isBlank()) {
            return 0.0;
        }

        CouponConfig config = VALID_COUPONS.get(couponCode.toUpperCase().trim());
        if (config == null) {
            throw new ResourceConflictException("Invalid coupon code: " + couponCode);
        }

        double discountAmount = (totalAmount * config.percentage) / 100.0;
        return Math.min(discountAmount, config.cap);
    }

    /**
     * Validates whether a coupon code is valid.
     *
     * @param couponCode the coupon code to validate
     * @return true if the coupon code is valid, false otherwise
     */
    public static boolean isValidCoupon(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return true; // No coupon is valid
        }
        return VALID_COUPONS.containsKey(couponCode.toUpperCase().trim());
    }

    /**
     * Gets the discount percentage for a valid coupon code.
     *
     * @param couponCode the coupon code
     * @return discount percentage, or empty if invalid
     */
    public static Optional<Double> getDiscountPercentage(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return Optional.empty();
        }
        CouponConfig config = VALID_COUPONS.get(couponCode.toUpperCase().trim());
        return config != null ? Optional.of(config.percentage) : Optional.empty();
    }

    /**
     * Gets the discount cap for a valid coupon code.
     *
     * @param couponCode the coupon code
     * @return discount cap, or empty if invalid
     */
    public static Optional<Double> getDiscountCap(String couponCode) {
        if (couponCode == null || couponCode.isBlank()) {
            return Optional.empty();
        }
        CouponConfig config = VALID_COUPONS.get(couponCode.toUpperCase().trim());
        return config != null ? Optional.of(config.cap) : Optional.empty();
    }
}

