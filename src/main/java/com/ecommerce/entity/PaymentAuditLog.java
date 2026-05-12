package com.ecommerce.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;

@Getter
@Setter
@Entity
@Table(name = "payment_audit_logs")
public class PaymentAuditLog {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "payment_id")
    @com.fasterxml.jackson.annotation.JsonIgnore
    private Payment payment;

    private String previousState;
    private String newState;
    private String actionBy;
    private String details;
    private LocalDateTime timestamp;
}
