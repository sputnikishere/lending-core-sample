package com.example.lending.core.paymentplan.impl.strategy;

import com.example.lending.core.paymentplan.PaymentDateProvider;
import com.example.lending.core.paymentplan.PaymentPlanCalculationStrategy;
import com.example.lending.core.paymentplan.PaymentPlanData;
import com.example.lending.core.paymentplan.PaymentPlanKeys;
import com.example.lending.core.product.CreditProduct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

/**
 * Payday loan (PDL): a single bullet payment of principal plus simple daily interest.
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class PDLPaymentPlanCalculationStrategy implements PaymentPlanCalculationStrategy {

    private final PaymentDateProvider dateProvider;

    @Override
    public Integer getStrategyKey() {
        return PaymentPlanKeys.PAYMENT_PLAN_CALCULATION_STRATEGY_PDL;
    }

    @Override
    public PaymentPlanData calculatePaymentPlan(BigDecimal loanAmount, CreditProduct product, LocalDate startDate, ChronoUnit periodType, int period, int term) {
        PaymentPlanData paymentPlanData = new PaymentPlanData();

        List<LocalDate> paymentDates = dateProvider.getPaymentDates(startDate, periodType, period, term, product.getPeriodDaysMax());
        if (paymentDates.isEmpty()) {
            throw new IllegalArgumentException("No payment dates found for generate payment plan");
        }

        LocalDate dayCounter = startDate;
        BigDecimal percentAmount = BigDecimal.ZERO;
        while (dayCounter.isBefore(paymentDates.getFirst())) {
            percentAmount = percentAmount.add(loanAmount.multiply(product.getRateDaily()).setScale(2, RoundingMode.UP));
            dayCounter = dayCounter.plusDays(1);
        }

        BigDecimal mainAmount = loanAmount.setScale(2, RoundingMode.DOWN);
        percentAmount = percentAmount.setScale(2, RoundingMode.DOWN);

        PaymentPlanData.PaymentPlanStepData stepData = getPaymentPlanStepData(paymentDates.getFirst(), mainAmount, percentAmount);

        paymentPlanData.setPaymentPlanSteps(List.of(stepData));

        return paymentPlanData;
    }

    /**
     * Recalculates the PDL plan after an early repayment.
     * Returns interest for the period (startDate+1 ... futureDates[0]) and the outstanding principal.
     * Historical accruals (up to and including startDate) are added by the caller.
     */
    @Override
    public PaymentPlanData recalculate(BigDecimal loanAmount, BigDecimal dailyRate,
                                       LocalDate startDate, List<LocalDate> futureDates) {
        if (futureDates.isEmpty()) {
            PaymentPlanData empty = new PaymentPlanData();
            empty.setPaymentPlanSteps(List.of());
            return empty;
        }
        LocalDate lastDate = futureDates.getFirst();
        LocalDate dayCounter = startDate.plusDays(1);
        BigDecimal percentAmount = BigDecimal.ZERO;
        while (dayCounter.isBefore(lastDate)) {
            percentAmount = percentAmount.add(loanAmount.multiply(dailyRate).setScale(2, RoundingMode.UP));
            dayCounter = dayCounter.plusDays(1);
        }
        percentAmount = percentAmount.setScale(2, RoundingMode.DOWN);
        BigDecimal mainAmount = loanAmount.setScale(2, RoundingMode.DOWN);

        PaymentPlanData data = new PaymentPlanData();
        data.setPaymentPlanSteps(List.of(getPaymentPlanStepData(lastDate, mainAmount, percentAmount)));
        return data;
    }

    private static PaymentPlanData.PaymentPlanStepData getPaymentPlanStepData(LocalDate paymentDate, BigDecimal mainAmount, BigDecimal percentAmount) {
        PaymentPlanData.PaymentPlanStepData stepData = new PaymentPlanData.PaymentPlanStepData();
        stepData.setPaymentDate(paymentDate);
        stepData.setMainAmount(mainAmount);
        stepData.setPaymentAmount(mainAmount);
        stepData.setMainDebtRemaining(BigDecimal.ZERO);


        if (percentAmount.compareTo(BigDecimal.ZERO) > 0) {
            stepData.setPercentAmount(percentAmount);
            stepData.setPaymentAmount(stepData.getPaymentAmount().add(percentAmount));
        } else {
            stepData.setPercentAmount(BigDecimal.ZERO);
        }
        return stepData;
    }
}
