package com.altoshaam.labelsystem.entity;

import jakarta.persistence.CascadeType;
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
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;

import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "label_item", indexes = {
        @Index(name = "idx_label_item_batch", columnList = "batch_id")
})
public class LabelItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "batch_id", nullable = false)
    private LabelBatch batch;

    @Column(nullable = false, length = 80)
    private String customerPoNumber;

    @Column(length = 80)
    private String invoiceNumber;

    @Column(length = 80)
    private String packageId;

    @Column(length = 80)
    private String itemNumber;

    @Column(nullable = false, length = 50)
    private String partNumber;

    @Column(columnDefinition = "TEXT", nullable = false)
    private String description;

    @Column(nullable = false)
    private int perPalletQuantity;

    @Column(nullable = false)
    private int totalQuantity;

    @Column(nullable = false)
    private int numberOfPallets;

    @Column(nullable = false)
    private int recommendedPrintQuantity;

    @Column(nullable = false)
    private int requestedPrintQuantity;

    @Column(nullable = false)
    private int labelUnits;

    @Column(nullable = false)
    private int copiesPerUnitDefault;

    @Column(length = 20)
    private String packingListNumber;

    @Column(length = 20)
    private String revisionLevel;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 40)
    private ShippingScenario shippingScenario;

    @Column(columnDefinition = "TEXT")
    private String warnings;

    @Column(columnDefinition = "TEXT")
    private String rawInputJson;

    @OneToMany(mappedBy = "labelItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<GeneratedLabel> generatedLabels = new ArrayList<>();

    public LabelItem() {
    }

    public Long getId() { return id; }
    public LabelBatch getBatch() { return batch; }
    public void setBatch(LabelBatch batch) { this.batch = batch; }
    public String getCustomerPoNumber() { return customerPoNumber; }
    public void setCustomerPoNumber(String customerPoNumber) { this.customerPoNumber = customerPoNumber; }
    public String getInvoiceNumber() { return invoiceNumber; }
    public void setInvoiceNumber(String invoiceNumber) { this.invoiceNumber = invoiceNumber; }
    public String getPackageId() { return packageId; }
    public void setPackageId(String packageId) { this.packageId = packageId; }
    public String getItemNumber() { return itemNumber; }
    public void setItemNumber(String itemNumber) { this.itemNumber = itemNumber; }
    public String getPartNumber() { return partNumber; }
    public void setPartNumber(String partNumber) { this.partNumber = partNumber; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public int getPerPalletQuantity() { return perPalletQuantity; }
    public void setPerPalletQuantity(int perPalletQuantity) { this.perPalletQuantity = perPalletQuantity; }
    public int getTotalQuantity() { return totalQuantity; }
    public void setTotalQuantity(int totalQuantity) { this.totalQuantity = totalQuantity; }
    public int getNumberOfPallets() { return numberOfPallets; }
    public void setNumberOfPallets(int numberOfPallets) { this.numberOfPallets = numberOfPallets; }
    public int getRecommendedPrintQuantity() { return recommendedPrintQuantity; }
    public void setRecommendedPrintQuantity(int recommendedPrintQuantity) { this.recommendedPrintQuantity = recommendedPrintQuantity; }
    public int getRequestedPrintQuantity() { return requestedPrintQuantity; }
    public void setRequestedPrintQuantity(int requestedPrintQuantity) { this.requestedPrintQuantity = requestedPrintQuantity; }
    public int getLabelUnits() { return labelUnits; }
    public void setLabelUnits(int labelUnits) { this.labelUnits = labelUnits; }
    public int getCopiesPerUnitDefault() { return copiesPerUnitDefault; }
    public void setCopiesPerUnitDefault(int copiesPerUnitDefault) { this.copiesPerUnitDefault = copiesPerUnitDefault; }
    public String getPackingListNumber() { return packingListNumber; }
    public void setPackingListNumber(String packingListNumber) { this.packingListNumber = packingListNumber; }
    public String getRevisionLevel() { return revisionLevel; }
    public void setRevisionLevel(String revisionLevel) { this.revisionLevel = revisionLevel; }
    public ShippingScenario getShippingScenario() { return shippingScenario; }
    public void setShippingScenario(ShippingScenario shippingScenario) { this.shippingScenario = shippingScenario; }
    public String getWarnings() { return warnings; }
    public void setWarnings(String warnings) { this.warnings = warnings; }
    public String getRawInputJson() { return rawInputJson; }
    public void setRawInputJson(String rawInputJson) { this.rawInputJson = rawInputJson; }
    public List<GeneratedLabel> getGeneratedLabels() { return generatedLabels; }
}
