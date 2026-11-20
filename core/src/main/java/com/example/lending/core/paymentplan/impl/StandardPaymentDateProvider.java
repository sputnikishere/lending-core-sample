package com.example.lending.core.paymentplan.impl;

import com.example.lending.core.paymentplan.PaymentDateProvider;
import com.example.lending.core.paymentplan.PaymentPlanKeys;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

/**
 * Standard payment date provider.
 * Produces payment dates on a fixed period (days or months).
 */
@Service
public class StandardPaymentDateProvider implements PaymentDateProvider {
    @Override
    public Integer getProviderCode() {
        return PaymentPlanKeys.PAYMENT_DATE_PROVIDER_STANDARD;
    }

    @Override
    public List<LocalDate> getPaymentDates(LocalDate startDate, ChronoUnit periodType, int period, int paymentsCount, int maxTermDays) {
        List<LocalDate> dates = new ArrayList<>(paymentsCount);
        if (periodType == ChronoUnit.MONTHS) {
            // Compute every payment relative to the start date to preserve the "anchor" day of month
            for (int i = 1; i <= paymentsCount; i++) {
                dates.add(startDate.plusMonths((long) period * i));
            }
        } else if (periodType == ChronoUnit.DAYS) {
            // For days it is also more correct to count from the start date (avoids accumulating drift)
            for (int i = 1; i <= paymentsCount; i++) {
                dates.add(startDate.plusDays((long) period * i));
            }
        } else {
            throw new UnsupportedOperationException("Unsupported period type: " + periodType);
        }

        // Cap the loan term in days so it never exceeds the product's maximum term
        if (!dates.isEmpty()) {
            LocalDate lastPaymentDate = dates.getLast();
            int daysBetweenExcludingLast = (int) ChronoUnit.DAYS.between(startDate, lastPaymentDate);
            int daysToSubtract = daysBetweenExcludingLast - maxTermDays;
            if (daysToSubtract > 0) {
                dates.set(dates.size() - 1, lastPaymentDate.minusDays(daysToSubtract));
            }
        }

        return dates;
    }
}
