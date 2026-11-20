package com.example.lending.core.paymentplan;

import com.example.lending.core.product.CreditProduct;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface PaymentPlanCalculationStrategy {

    Integer getStrategyKey();

    /**
     * Calculates the repayment schedule for a loan.
     *
     * @param loanAmount    loan amount
     * @param product       credit product
     * @param startDate     repayment start date
     * @param periodType    period unit (e.g. months, days)
     * @param period        number of period units between payments
     * @param paymentsCount number of payments
     * @return repayment schedule data
     */
    PaymentPlanData calculatePaymentPlan(BigDecimal loanAmount, CreditProduct product, LocalDate startDate, ChronoUnit periodType, int period, int paymentsCount);

    /**
     * Recalculates schedule steps over an already known list of dates.
     * Used for early (partial) repayment, when the payment dates are fixed.
     *
     * @param loanAmount  outstanding principal as of the recalculation date
     * @param dailyRate   daily interest rate
     * @param startDate   period start date (the early repayment date)
     * @param futureDates ordered list of future payment dates
     * @return the new schedule data (steps only, no cost-of-credit metrics)
     */
    default PaymentPlanData recalculate(BigDecimal loanAmount, BigDecimal dailyRate,
                                        LocalDate startDate, List<LocalDate> futureDates) {
        throw new UnsupportedOperationException(
                "recalculate not supported for strategy " + getStrategyKey());
    }
}
