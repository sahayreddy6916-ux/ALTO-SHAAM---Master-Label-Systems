package com.altoshaam.labelsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

import java.time.Instant;

@Entity
@Table(name = "generated_label",
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_generated_label_serial", columnNames = "serial_number")
        },
        indexes = {
                @Index(name = "idx_generated_label_item", columnList = "label_item_id")
        })
public class GeneratedLabel {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "label_item_id", nullable = false)
    private LabelItem labelItem;

    @Column(name = "serial_number", nullable = false, length = 15)
    private String serialNumber;

    @Column(name = "barcode_data", nullable = false, length = 17)
    private String barcodeData;

    @Column(nullable = false)
    private int quantity;

    @Column(nullable = false)
    private int copyCount;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private PrintStatus status;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant printedAt;

    public GeneratedLabel() {
    }

    public Long getId() { return id; }
    public LabelItem getLabelItem() { return labelItem; }
    public void setLabelItem(LabelItem labelItem) { this.labelItem = labelItem; }
    public String getSerialNumber() { return serialNumber; }
    public void setSerialNumber(String serialNumber) { this.serialNumber = serialNumber; }
    public String getBarcodeData() { return barcodeData; }
    public void setBarcodeData(String barcodeData) { this.barcodeData = barcodeData; }
    public int getQuantity() { return quantity; }
    public void setQuantity(int quantity) { this.quantity = quantity; }
    public int getCopyCount() { return copyCount; }
    public void setCopyCount(int copyCount) { this.copyCount = copyCount; }
    public PrintStatus getStatus() { return status; }
    public void setStatus(PrintStatus status) { this.status = status; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPrintedAt() { return printedAt; }
    public void setPrintedAt(Instant printedAt) { this.printedAt = printedAt; }
}
