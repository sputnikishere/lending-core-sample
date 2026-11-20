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
import java.math.MathContext;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class AnnuityPaymentPlanCalculationStrategy implements PaymentPlanCalculationStrategy {

    private static final MathContext mc = MathContext.DECIMAL128;

    private final PaymentDateProvider dateProvider;

    @Override
    public Integer getStrategyKey() {
        return PaymentPlanKeys.PAYMENT_PLAN_CALCULATION_STRATEGY_ANNUITY;
    }

    @Override
    public PaymentPlanData calculatePaymentPlan(BigDecimal loanAmount, CreditProduct product,
                                                LocalDate startDate, ChronoUnit periodType,
                                                int period, int term) {

        List<LocalDate> paymentDates = dateProvider.getPaymentDates(startDate, periodType, period, term, product.getPeriodDaysMax());
        if (paymentDates.isEmpty()) {
            throw new IllegalArgumentException("No payment dates found for generate payment plan");
        }

        AnnuityPaymentPlanData annuityPaymentData =
                calculateAnnuity(loanAmount, product.getRateDaily(), startDate, paymentDates);

        PaymentPlanData paymentPlanData = new PaymentPlanData();
        paymentPlanData.setMonthlyPayment(annuityPaymentData.monthlyPayment);
        paymentPlanData.setLastPayment(annuityPaymentData.lastPayment);
        paymentPlanData.setPaymentPlanSteps(calculatePaymentSteps(
                loanAmount, startDate, paymentDates, product.getRateDaily(), annuityPaymentData));
        return paymentPlanData;
    }

    @Override
    public PaymentPlanData recalculate(BigDecimal loanAmount, BigDecimal dailyRate,
                                       LocalDate startDate, List<LocalDate> futureDates) {
        AnnuityPaymentPlanData annuityData = calculateAnnuity(loanAmount, dailyRate, startDate, futureDates);
        PaymentPlanData data = new PaymentPlanData();
        data.setMonthlyPayment(annuityData.monthlyPayment());
        data.setLastPayment(annuityData.lastPayment());
        data.setPaymentPlanSteps(calculatePaymentSteps(loanAmount, startDate, futureDates, dailyRate, annuityData));
        return data;
    }

    public List<PaymentPlanData.PaymentPlanStepData> calculatePaymentSteps(
            BigDecimal loanAmount,
            LocalDate startDate,
            List<LocalDate> paymentDates,
            BigDecimal dailyRate,
            AnnuityPaymentPlanData annuityPaymentData
    ) {
        List<PaymentPlanData.PaymentPlanStepData> paymentPlanSteps = new ArrayList<>();

        BigDecimal remaining = loanAmount;
        BigDecimal interestAccrued = BigDecimal.ZERO;
        LocalDate cursor = startDate;

        for (int i = 0; i < paymentDates.size(); i++) {
            LocalDate payDate = paymentDates.get(i);
            long days = ChronoUnit.DAYS.between(cursor, payDate);
            for (long j = 0; j < days; j++) {
                interestAccrued = interestAccrued.add(remaining.multiply(dailyRate).setScale(2, RoundingMode.UP), mc);
            }

            boolean isLast = (i == paymentDates.size() - 1);
            BigDecimal payment = isLast ? annuityPaymentData.lastPayment : annuityPaymentData.monthlyPayment;

            BigDecimal interestPaid = payment.min(interestAccrued.setScale(2, RoundingMode.UP));
            interestAccrued = interestAccrued.subtract(interestPaid, mc);

            BigDecimal mainPay = payment.subtract(interestPaid, mc);
            if (mainPay.compareTo(remaining) > 0) {
                mainPay = remaining;
            }
            remaining = remaining.subtract(mainPay, mc);

            paymentPlanSteps.add(new PaymentPlanData.PaymentPlanStepData(
                    payDate, payment, mainPay, interestPaid, remaining));

            cursor = payDate.plusDays(1);
        }
        return paymentPlanSteps;
    }


    public AnnuityPaymentPlanData calculateAnnuity(
            BigDecimal loanAmount,
            BigDecimal dailyRate,
            LocalDate startDate,
            List<LocalDate> paymentDates
    ) {

        // Find the payment amount by binary search
        BigDecimal low = BigDecimal.ZERO;
        BigDecimal high = loanAmount.multiply(BigDecimal.valueOf(5));
        BigDecimal mid;

        for (int iter = 0; iter < 200; iter++) {
            mid = low.add(high, mc).divide(BigDecimal.valueOf(2), mc);
            EndBalanceOverpay endBalanceOverpay = simulateEndBalanceWithOverpay(
                    loanAmount, dailyRate, startDate, paymentDates, mid);

            // endBalance > 0 -> underpaid (principal remains), increase the payment
            // endBalance < 0 -> overpaid, decrease the payment
            if (endBalanceOverpay.endBalance().signum() > 0) {
                low = mid;
            } else {
                high = mid;
            }

            // Stop once the search interval has converged
            if (high.subtract(low, mc).abs().compareTo(new BigDecimal("1e-12")) < 0) {
                break;
            }
        }

        // Round the payment up to whole cents so that the residual is never positive
        BigDecimal payment = high.setScale(2, RoundingMode.UP);
        EndBalanceOverpay endBalanceOverpay = simulateEndBalanceWithOverpay(loanAmount, dailyRate, startDate, paymentDates, payment);
        BigDecimal cent = new BigDecimal("0.01");
        // Bump the payment by one cent until the residual turns non-positive (a small overpay absorbed by the last payment)
        while (endBalanceOverpay.endBalance().signum() > 0) {
            payment = payment.add(cent);
            endBalanceOverpay = simulateEndBalanceWithOverpay(loanAmount, dailyRate, startDate, paymentDates, payment);
        }

        return new AnnuityPaymentPlanData(payment, payment.subtract(endBalanceOverpay.overpay()));
    }

    private static EndBalanceOverpay simulateEndBalanceWithOverpay(
            BigDecimal loanAmount,
            BigDecimal dailyRate,
            LocalDate startDate,
            List<LocalDate> paymentDates,
            BigDecimal payment
    ) {

        BigDecimal remaining = loanAmount;
        BigDecimal interestAccrued = BigDecimal.ZERO;
        BigDecimal overpay = BigDecimal.ZERO;

        LocalDate cursor = startDate;

        for (LocalDate payDate : paymentDates) {
            long days = ChronoUnit.DAYS.between(cursor, payDate);
            for (long i = 0; i < days; i++) {
                interestAccrued = interestAccrued.add(remaining.multiply(dailyRate).setScale(2, RoundingMode.UP), mc);
            }

            // Apply the payment: interest first, then principal
            BigDecimal interestPaid = payment.min(interestAccrued.setScale(2, RoundingMode.UP));
            interestAccrued = interestAccrued.subtract(interestPaid, mc);

            BigDecimal principalPaid = payment.subtract(interestPaid, mc);
            if (principalPaid.compareTo(remaining) > 0) {
                principalPaid = remaining;
            }
            remaining = remaining.subtract(principalPaid, mc);

            // If a "tail" of the payment is left after principal is fully repaid, count it as overpay
            BigDecimal used = interestPaid.add(principalPaid, mc);
            BigDecimal leftover = payment.subtract(used, mc);
            if (leftover.signum() > 0 && remaining.signum() == 0) {
                overpay = overpay.add(leftover, mc);
            }

            cursor = payDate.plusDays(1);
        }

        // Final residual: positive -> underpaid, negative -> overpaid
        return new EndBalanceOverpay(remaining.subtract(overpay, mc), overpay);
    }

    private record EndBalanceOverpay(
            BigDecimal endBalance,
            BigDecimal overpay
    ) {}

    public record AnnuityPaymentPlanData(
            BigDecimal monthlyPayment,
            BigDecimal lastPayment
    ) {}
}
