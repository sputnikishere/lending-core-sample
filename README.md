# Lending Core — code sample

A small excerpt from a production modular lending platform (loan origination and servicing for
payday loans and installment/annuity loans, payments, collections). It shows how I structure
domain logic, money calculations, concurrency and integration code. The full platform
architecture is described in [ARCHITECTURE.md](ARCHITECTURE.md).

Package names are anonymized and a few persistence lookups are reduced to small interfaces.
It is **an excerpt, not a runnable project**: there is no build file and the surrounding services
are left out. The algorithms are unchanged.

**Stack:** Java 21, Spring Boot 3.5, PostgreSQL, Kafka, Redis, Flowable (BPMN), Lombok, JUnit 5.

## Layout

```
core/          com.example.lending.core
  paymentplan/   repayment schedule strategies (PDL, annuity) and payment date providers (+ tests)
calculations/  com.example.lending.calculations
  executor/      queued interest-accrual executor with per-loan locking
collection/    com.example.lending.collection
  assignment/    collector roster and least-loaded case assignment
```

## Design notes

### `core.paymentplan` — repayment schedules
- **Strategy pattern.** `PaymentPlanCalculationStrategy` is chosen by a product setting, so a
  product picks its schedule algorithm (PDL single payment vs. annuity) by configuration. The
  same interface handles recalculation after an early partial repayment.
- **Date generation is a separate seam** (`PaymentDateProvider`). Dates are anchored to the
  start date, not chained, so month-end loans don't drift (Jan 31 → Feb 28 → Mar 31), and the
  final date is clamped to the product's maximum term.
- **Annuity solver.** Instead of the closed-form formula (which assumes equal periods), the
  payment is found by **binary search over a day-by-day simulation** using the same rounding as
  the servicing engine. It is rounded up to whole cents and bumped one cent at a time until the
  simulated residual is never positive; any overpay is absorbed by a smaller last payment, so the
  schedule always fully amortizes.

### `calculations.executor` — background processing
- **Per-loan locking.** A fixed worker pool drains a blocking queue; every calculation runs
  under a per-loan `ReentrantLock`, so different loans are processed in parallel while updates
  to the same loan are serialized. Synchronous (API-triggered) calls use the same lock path.
- Graceful shutdown with a configurable drain timeout.

### `collection.assignment` — collections
- **Least-loaded, deterministic assignment.** A new case goes to the collector with the fewest
  open cases; ties are broken by login, so the outcome is reproducible and testable.
- **Cached roster with graceful degradation.** The collector list comes from the employee
  directory service, cached with a TTL and double-checked locking. If the directory is down,
  the last snapshot is used, so assignment never blocks a case from being queued.
