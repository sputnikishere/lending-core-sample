package com.example.lending.collection.domain;

import com.example.lending.collection.enums.CollectionStatusEnum;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * A collection case - a loan that has entered the collections team's work queue.
 * Trimmed to the fields used by assignment; the full entity also carries
 * borrower, overdue amounts, promises to pay and audit columns.
 */
@Entity
@Getter
@Setter
@NoArgsConstructor
@Table(name = "collection_application")
public class CollectionApplication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * The loan under collection. Referenced by id only, without a JPA relation:
     * the collections service does not own the loan schema.
     */
    @Column(name = "credit_id", nullable = false, unique = true)
    private Long creditId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private CollectionStatusEnum status;

    @Column(name = "assignee_user_id")
    private UUID assigneeUserId;

    @Column(name = "assignee_login")
    private String assigneeLogin;

    @Column(name = "assigned_at")
    private LocalDateTime assignedAt;

    /**
     * Assigns the case to a collector.
     *
     * @param userId collector identifier
     * @param login  collector login
     * @param at     assignment moment
     */
    public void assignTo(UUID userId, String login, LocalDateTime at) {
        this.assigneeUserId = userId;
        this.assigneeLogin = login;
        this.assignedAt = at;
    }

    /**
     * Whether the case has an assignee.
     *
     * @return true if an assignee is set
     */
    public boolean isAssigned() {
        return assigneeUserId != null;
    }
}
