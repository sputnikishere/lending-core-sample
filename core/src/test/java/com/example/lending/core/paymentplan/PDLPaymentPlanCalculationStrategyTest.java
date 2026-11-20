package com.example.lending.core.paymentplan;

import com.example.lending.core.paymentplan.impl.StandardPaymentDateProvider;
import com.example.lending.core.paymentplan.impl.strategy.PDLPaymentPlanCalculationStrategy;
import com.example.lending.core.product.CreditProduct;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;

class PDLPaymentPlanCalculationStrategyTest {

    private final PDLPaymentPlanCalculationStrategy strategy =
            new PDLPaymentPlanCalculationStrategy(new StandardPaymentDateProvider());

    @Test
    void test_PDL_1() {

        // PDL: amount 14,000, 13 days, 0.8% per day,
        // start 2025-08-15, maturity 2025-08-28

        PaymentPlanData plan = strategy.calculatePaymentPlan(
                BigDecimal.valueOf(14000), pdlProduct(),
                LocalDate.of(2025, 8, 15), ChronoUnit.DAYS, 13, 1);

        List<PaymentPlanData.PaymentPlanStepData> steps = plan.getPaymentPlanSteps();
        assertEquals(1, steps.size());
        assertEquals(LocalDate.of(2025, 8, 28), steps.getFirst().getPaymentDate());
        assertEquals(BigDecimal.valueOf(15456).setScale(2, RoundingMode.DOWN), steps.getFirst().getPaymentAmount());
    }

    @Test
    void test_PDL_2() {

        // PDL: amount 10,000, 10 days, 0.8% per day,
        // start 2025-08-15, maturity 2025-08-25

        PaymentPlanData plan = strategy.calculatePaymentPlan(
                BigDecimal.valueOf(10000), pdlProduct(),
                LocalDate.of(2025, 8, 15), ChronoUnit.DAYS, 10, 1);

        List<PaymentPlanData.PaymentPlanStepData> steps = plan.getPaymentPlanSteps();
        assertEquals(1, steps.size());
        assertEquals(LocalDate.of(2025, 8, 25), steps.getFirst().getPaymentDate());
        assertEquals(BigDecimal.valueOf(10800).setScale(2, RoundingMode.DOWN), steps.getFirst().getPaymentAmount());
    }

    private static CreditProduct pdlProduct() {
        return new CreditProduct() {
            @Override
            public BigDecimal getRateDaily() {
                return new BigDecimal("0.008");
            }

            @Override
            public Integer getPeriodDaysMax() {
                return 15;
            }
        };
    }
}
