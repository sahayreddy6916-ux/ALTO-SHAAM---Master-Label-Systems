package com.altoshaam.labelsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import jakarta.persistence.Version;

@Entity
@Table(name = "app_settings")
public class AppSetting {

    @Id
    private Long id;

    @Column(nullable = false)
    private String supplierId = "";

    @Column(nullable = false)
    private String supplierName = "";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String shipFrom = "";

    @Column(columnDefinition = "TEXT", nullable = false)
    private String shipTo = "";

    @Column(nullable = false)
    private String packingListSource = "AUTO";

    @Column(nullable = false)
    private String defaultRevisionLevel = "";

    @Version
    private Long version;

    public AppSetting() {
    }

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getSupplierId() { return supplierId; }
    public void setSupplierId(String supplierId) { this.supplierId = supplierId; }
    public String getSupplierName() { return supplierName; }
    public void setSupplierName(String supplierName) { this.supplierName = supplierName; }
    public String getShipFrom() { return shipFrom; }
    public void setShipFrom(String shipFrom) { this.shipFrom = shipFrom; }
    public String getShipTo() { return shipTo; }
    public void setShipTo(String shipTo) { this.shipTo = shipTo; }
    public String getPackingListSource() { return packingListSource; }
    public void setPackingListSource(String packingListSource) { this.packingListSource = packingListSource; }
    public String getDefaultRevisionLevel() { return defaultRevisionLevel; }
    public void setDefaultRevisionLevel(String defaultRevisionLevel) { this.defaultRevisionLevel = defaultRevisionLevel; }
    public Long getVersion() { return version; }
}
