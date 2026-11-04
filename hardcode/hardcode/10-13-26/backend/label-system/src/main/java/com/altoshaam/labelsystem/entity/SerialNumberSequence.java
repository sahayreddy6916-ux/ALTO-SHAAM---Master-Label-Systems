package com.altoshaam.labelsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "serial_number_sequence", uniqueConstraints = {
        @UniqueConstraint(name = "uk_serial_sequence_supplier", columnNames = "supplier_id")
})
public class SerialNumberSequence {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "supplier_id", nullable = false, length = 14)
    private String supplierId;

    @Column(name = "current_sequence", nullable = false)
    private long currentSequence;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;

    public SerialNumberSequence() {
    }

    public Long getId() { return id; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public long getCurrentSequence() { return currentSequence; }
    public void setCurrentSequence(long currentSequence) { this.currentSequence = currentSequence; }
    public Instant getUpdatedAt() { return updatedAt; }
    public void setUpdatedAt(Instant updatedAt) { this.updatedAt = updatedAt; }
}
