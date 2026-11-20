package com.example.lending.core.paymentplan;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

public interface PaymentDateProvider {

    Integer getProviderCode();

    List<LocalDate> getPaymentDates(LocalDate startDate, ChronoUnit periodType, int period, int paymentsCount, int maxTermDays);
}
