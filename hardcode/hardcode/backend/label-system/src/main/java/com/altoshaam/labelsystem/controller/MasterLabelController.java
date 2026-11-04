package com.altoshaam.labelsystem.controller;

import com.altoshaam.labelsystem.dto.BatchSummaryResponse;
import com.altoshaam.labelsystem.dto.GenerationRequest;
import com.altoshaam.labelsystem.dto.GenerationResponse;
import com.altoshaam.labelsystem.dto.MasterLabelRowRequest;
import com.altoshaam.labelsystem.dto.ProcessResponse;
import com.altoshaam.labelsystem.entity.LabelBatch;
import com.altoshaam.labelsystem.entity.GeneratedLabel;
import com.altoshaam.labelsystem.service.MasterLabelService;
import jakarta.validation.Valid;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.CacheControl;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/master-labels")
public class MasterLabelController {

    private final MasterLabelService service;

    public MasterLabelController(MasterLabelService service) {
        this.service = service;
    }

    @PostMapping("/preview")
    public ProcessResponse preview(@RequestBody List<MasterLabelRowRequest> rows) {
        return service.preview(rows);
    }

    @PostMapping("/generate")
    public GenerationResponse generate(@Valid @RequestBody GenerationRequest request) {
        return service.generate(request);
    }

    @GetMapping("/history")
    public List<BatchSummaryResponse> history(@RequestParam(defaultValue = "50") int limit) {
        return service.history(limit);
    }

    @GetMapping("/batches/{batchId}/pdf")
    public ResponseEntity<ByteArrayResource> pdf(@PathVariable Long batchId) {
        byte[] bytes = service.getPdf(batchId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("master-label-batch-" + batchId + ".pdf").build().toString())
                .body(new ByteArrayResource(bytes));
    }

    @PutMapping("/batches/{batchId}/mark-printed")
    public ResponseEntity<Void> markPrinted(@PathVariable Long batchId) {
        service.markPrinted(batchId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/batches/{batchId}/reprint")
    public ResponseEntity<ByteArrayResource> reprint(@PathVariable Long batchId) {
        byte[] bytes = service.reprint(batchId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .cacheControl(CacheControl.noStore())
                .header(HttpHeaders.CONTENT_DISPOSITION, ContentDisposition.inline()
                        .filename("master-label-reprint-" + batchId + ".pdf").build().toString())
                .body(new ByteArrayResource(bytes));
    }

    @GetMapping("/batches/{batchId}")
    public Map<String, Object> detail(@PathVariable Long batchId) {
        LabelBatch batch = service.getBatch(batchId);
        List<Map<String, Object>> items = new ArrayList<>();
        batch.getItems().forEach(item -> {
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("id", item.getId());
            row.put("customerPoNumber", item.getCustomerPoNumber());
            row.put("invoiceNumber", item.getInvoiceNumber());
            row.put("packageId", item.getPackageId());
            row.put("itemNumber", item.getItemNumber());
            row.put("partNumber", item.getPartNumber());
            row.put("description", item.getDescription());
            row.put("perPalletQuantity", item.getPerPalletQuantity());
            row.put("totalQuantity", item.getTotalQuantity());
            row.put("numberOfPallets", item.getNumberOfPallets());
            row.put("recommendedPrintQuantity", item.getRecommendedPrintQuantity());
            row.put("requestedPrintQuantity", item.getRequestedPrintQuantity());
            row.put("packingListNumber", item.getPackingListNumber());
            row.put("revisionLevel", item.getRevisionLevel());
            row.put("shippingScenario", item.getShippingScenario().name());
            row.put("warnings", item.getWarnings());
            row.put("labels", item.getGeneratedLabels().stream().map(this::labelSummary).toList());
            items.add(row);
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", batch.getId());
        response.put("batchNumber", batch.getBatchNumber());
        response.put("status", batch.getStatus().name());
        response.put("totalRows", batch.getTotalRows());
        response.put("totalLabelUnits", batch.getTotalLabelUnits());
        response.put("totalPhysicalLabels", batch.getTotalPhysicalLabels());
        response.put("createdAt", batch.getCreatedAt());
        response.put("printedAt", batch.getPrintedAt());
        response.put("items", items);
        return response;
    }

    private Map<String, Object> labelSummary(GeneratedLabel label) {
        Map<String, Object> map = new LinkedHashMap<>();
        map.put("id", label.getId());
        map.put("serialNumber", label.getSerialNumber());
        map.put("barcodeData", label.getBarcodeData());
        map.put("quantity", label.getQuantity());
        map.put("copyCount", label.getCopyCount());
        map.put("status", label.getStatus().name());
        map.put("createdAt", label.getCreatedAt());
        map.put("printedAt", label.getPrintedAt());
        return map;
    }
}
