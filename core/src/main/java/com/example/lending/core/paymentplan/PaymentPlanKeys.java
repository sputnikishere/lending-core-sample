package com.example.lending.core.paymentplan;

/**
 * Reference codes used to select a strategy / date provider from product configuration.
 */
public final class PaymentPlanKeys {

    public static final int PAYMENT_DATE_PROVIDER_STANDARD = 1;

    public static final int PAYMENT_PLAN_CALCULATION_STRATEGY_ANNUITY = 1;
    public static final int PAYMENT_PLAN_CALCULATION_STRATEGY_PDL     = 2;

    private PaymentPlanKeys() {
    }
}
