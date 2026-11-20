package com.example.lending.core.product;

import java.math.BigDecimal;

/**
 * The subset of a credit product's configuration that repayment schedule strategies need.
 * In the full platform this is a persisted, versioned product definition.
 */
public interface CreditProduct {

    /** Daily interest rate as a fraction, e.g. 0.008 = 0.8% per day. */
    BigDecimal getRateDaily();

    /** Maximum loan term in days allowed by the product. */
    Integer getPeriodDaysMax();
}
