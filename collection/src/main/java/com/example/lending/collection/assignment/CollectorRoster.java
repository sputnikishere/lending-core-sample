package com.example.lending.collection.assignment;

import com.example.lending.collection.model.CollectorData;

import java.util.List;

/**
 * Registry of collector employees available for case assignment.
 */
public interface CollectorRoster {

    /**
     * Returns the current list of collectors. The implementation caches the response - assignment
     * runs on every new case, so there is no need to call the employee directory each time.
     *
     * @return list of collectors, empty if there are no active employees
     */
    List<CollectorData> collectors();

    /**
     * Drops the roster cache - called when assignment found no employees,
     * so that the next attempt sees fresh data.
     */
    void invalidate();
}
