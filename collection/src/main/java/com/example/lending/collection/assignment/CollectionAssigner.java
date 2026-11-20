package com.example.lending.collection.assignment;

import com.example.lending.collection.domain.CollectionApplication;

/**
 * Strategy for distributing collection cases among employees.
 * A dedicated interface leaves room to replace the assignee selection rule
 * (e.g. with a BPMN process in the workflow service) without touching the rest of the code.
 */
public interface CollectionAssigner {

    /**
     * Selects and sets the assignee of a case.
     *
     * @param application collection case
     * @return true if an assignee was set; false if no suitable employee was found
     */
    boolean assign(CollectionApplication application);
}
