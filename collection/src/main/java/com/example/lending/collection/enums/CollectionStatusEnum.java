package com.example.lending.collection.enums;

import lombok.Getter;

import java.util.Set;

/**
 * Status of a collection case - the lifecycle of a collector's work with a delinquent borrower.
 */
@Getter
public enum CollectionStatusEnum {

    NEW("New"),
    IN_PROGRESS("In progress"),
    PROMISE_TO_PAY("Promise to pay"),
    PARTIALLY_PAID("Partially paid"),
    UNREACHABLE("Borrower unreachable"),
    LEGAL("Referred to legal"),
    CLOSED_PAID("Closed - paid"),
    CLOSED_WRITTEN_OFF("Closed - written off");

    /**
     * Statuses in which a case is considered finished and is not shown in the work queue.
     */
    private static final Set<CollectionStatusEnum> TERMINAL = Set.of(CLOSED_PAID, CLOSED_WRITTEN_OFF);

    private final String description;

    CollectionStatusEnum(String description) {
        this.description = description;
    }

    /**
     * Whether the status is terminal - the collector's work on the case is done.
     *
     * @return true if the case is closed
     */
    public boolean isTerminal() {
        return TERMINAL.contains(this);
    }
}
