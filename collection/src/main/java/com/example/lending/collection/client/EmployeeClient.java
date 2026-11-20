package com.example.lending.collection.client;

import com.example.lending.collection.model.CollectorData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

/**
 * Client for the employee directory service's internal API - the single source of the
 * employee registry; the collections service never reads user tables directly.
 */
@FeignClient(name = "employeeClient", url = "${employee-service.url}")
public interface EmployeeClient {

    /**
     * Returns active employees permitted to work collection cases.
     *
     * @return list of collectors
     */
    @GetMapping("/internal/collection/collectors")
    List<CollectorData> collectors();
}
