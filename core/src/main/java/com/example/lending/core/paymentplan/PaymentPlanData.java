package com.example.lending.core.paymentplan;

import com.example.lending.core.utils.DateTimeUtils;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@AllArgsConstructor
@NoArgsConstructor
public class PaymentPlanData {

    private BigDecimal monthlyPayment;
    private BigDecimal lastPayment;

    private List<PaymentPlanStepData> paymentPlanSteps = new ArrayList<>();

    @Getter
    @Setter
    @AllArgsConstructor
    @NoArgsConstructor
    public static class PaymentPlanStepData {
        @JsonFormat(pattern = DateTimeUtils.DATE_PATTERN)
        private LocalDate paymentDate;
        private BigDecimal paymentAmount;
        private BigDecimal mainAmount;
        private BigDecimal percentAmount;
        private BigDecimal mainDebtRemaining;
    }

    public PaymentPlanStepData findNearestOrLatestPaymentPlanStateData(LocalDate target) {
        if (paymentPlanSteps == null || paymentPlanSteps.isEmpty()) {
            return null;
        }

        PaymentPlanStepData nearest = null;
        PaymentPlanStepData latest = null;

        for (PaymentPlanStepData step : paymentPlanSteps) {
            if (latest == null || step.getPaymentDate().isAfter(latest.getPaymentDate())) {
                latest = step;
            }

            if (!step.getPaymentDate().isBefore(target)) {
                if (nearest == null || step.getPaymentDate().isBefore(nearest.getPaymentDate())) {
                    nearest = step;
                }
            }
        }

        return nearest != null ? nearest : latest;
    }
}
