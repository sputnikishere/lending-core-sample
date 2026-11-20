package com.example.lending.core.paymentplan;

import com.example.lending.core.paymentplan.impl.StandardPaymentDateProvider;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class StandardPaymentDateProviderTest {

    private final StandardPaymentDateProvider provider = new StandardPaymentDateProvider();

    @Test
    void testMonthlyPayments() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        List<LocalDate> dates = provider.getPaymentDates(start, ChronoUnit.MONTHS, 1, 3, 1000);
        assertEquals(3, dates.size());
        assertEquals(LocalDate.of(2025, 2, 1), dates.get(0));
        assertEquals(LocalDate.of(2025, 3, 1), dates.get(1));
        assertEquals(LocalDate.of(2025, 4, 1), dates.get(2));
    }

    @Test
    void testDailyPayments() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        List<LocalDate> dates = provider.getPaymentDates(start, ChronoUnit.DAYS, 7, 2, 1000);
        assertEquals(2, dates.size());
        assertEquals(start.plusDays(7), dates.get(0));
        assertEquals(start.plusDays(14), dates.get(1));
    }

    @Test
    void testMaxTermDaysAdjustment() {
        LocalDate start = LocalDate.of(2025, 1, 1);
        int period = 30;
        int paymentsCount = 2;
        int maxTermDays = 45;

        List<LocalDate> dates = provider.getPaymentDates(start, ChronoUnit.DAYS, period, paymentsCount, maxTermDays);
        assertEquals(paymentsCount, dates.size());

        LocalDate expectedLast = start.plusDays(maxTermDays);
        assertEquals(expectedLast, dates.getLast());

        assertEquals(start.plusDays(period), dates.getFirst());
    }

    @Test
    void testDay31WithFebruary() {
        LocalDate start = LocalDate.of(2025, 12, 31);
        List<LocalDate> dates = provider.getPaymentDates(start, ChronoUnit.MONTHS, 1, 6, 1000);
        assertEquals(LocalDate.of(2026, 1, 31), dates.get(0));
        assertEquals(LocalDate.of(2026, 2, 28), dates.get(1));
        assertEquals(LocalDate.of(2026, 3, 31), dates.get(2));
        assertEquals(LocalDate.of(2026, 4, 30), dates.get(3));
        assertEquals(LocalDate.of(2026, 5, 31), dates.get(4));
        assertEquals(LocalDate.of(2026, 6, 30), dates.get(5));
    }
}

