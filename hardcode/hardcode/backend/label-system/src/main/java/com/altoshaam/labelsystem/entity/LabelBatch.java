package com.altoshaam.labelsystem.entity;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "label_batch")
public class LabelBatch {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 80)
    private String batchNumber;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private BatchStatus status;

    @Column(nullable = false)
    private int totalRows;

    @Column(nullable = false)
    private int totalLabelUnits;

    @Column(nullable = false)
    private int totalPhysicalLabels;

    @Column(columnDefinition = "TEXT")
    private String pdfPath;

    @Column(nullable = false)
    private Instant createdAt;

    @Column
    private Instant printedAt;

    @OneToMany(mappedBy = "batch", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<LabelItem> items = new ArrayList<>();

    public LabelBatch() {
    }

    public Long getId() { return id; }
    public String getBatchNumber() { return batchNumber; }
    public void setBatchNumber(String batchNumber) { this.batchNumber = batchNumber; }
    public BatchStatus getStatus() { return status; }
    public void setStatus(BatchStatus status) { this.status = status; }
    public int getTotalRows() { return totalRows; }
    public void setTotalRows(int totalRows) { this.totalRows = totalRows; }
    public int getTotalLabelUnits() { return totalLabelUnits; }
    public void setTotalLabelUnits(int totalLabelUnits) { this.totalLabelUnits = totalLabelUnits; }
    public int getTotalPhysicalLabels() { return totalPhysicalLabels; }
    public void setTotalPhysicalLabels(int totalPhysicalLabels) { this.totalPhysicalLabels = totalPhysicalLabels; }
    public String getPdfPath() { return pdfPath; }
    public void setPdfPath(String pdfPath) { this.pdfPath = pdfPath; }
    public Instant getCreatedAt() { return createdAt; }
    public void setCreatedAt(Instant createdAt) { this.createdAt = createdAt; }
    public Instant getPrintedAt() { return printedAt; }
    public void setPrintedAt(Instant printedAt) { this.printedAt = printedAt; }
    public List<LabelItem> getItems() { return items; }
}
