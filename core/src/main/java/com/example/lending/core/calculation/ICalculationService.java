package com.example.lending.core.calculation;

import java.time.LocalDate;

/**
 * Daily accrual engine: computes interest / penalty accruals for a loan on a given date.
 */
public interface ICalculationService {

    void executeCalculation(Long creditId, LocalDate date);
}
