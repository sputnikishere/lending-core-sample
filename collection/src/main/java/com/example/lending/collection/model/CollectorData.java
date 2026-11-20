package com.example.lending.collection.model;

import java.util.UUID;

/**
 * A collector (collections agent) from the employee directory service.
 *
 * @param id    user identifier
 * @param login user login
 */
public record CollectorData(UUID id, String login) {
}
