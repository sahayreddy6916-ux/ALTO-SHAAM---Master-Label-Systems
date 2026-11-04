package com.altoshaam.labelsystem.service;

import com.altoshaam.labelsystem.dto.MasterLabelRowRequest;
import com.altoshaam.labelsystem.dto.ProcessedRowResponse;
import com.altoshaam.labelsystem.entity.ShippingScenario;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.List;

@Service
public class MasterLabelRuleService {

    private static final int MAX_PALLETS = 10_000;
    private static final int MAX_PRINT_QUANTITY = 100_000;

    public ProcessedRowResponse process(
            MasterLabelRowRequest request,
            String defaultRevisionLevel,
            String packingListSource
    ) {
        List<String> errors = new ArrayList<>();
        List<String> warnings = new ArrayList<>();

        String po = clean(request.customerPoNumber());
        String invoice = clean(request.invoiceNumber());
        String pkgId = clean(request.packageId());
        String item = clean(request.itemNumber());
        String part = clean(request.partNumber());
        String description = clean(request.description());
        int perPallet = safeInt(request.perPalletQuantity());
        int totalQty = safeInt(request.totalQuantity());
        int pallets = safeInt(request.numberOfPallets());

        if (po.isBlank()) errors.add("Customer PO is required.");
        if (part.isBlank()) errors.add("Part Number is required.");
        if (description.isBlank()) errors.add("Item/Service Description is required.");
        if (perPallet <= 0) errors.add("Per Pallet Qty must be greater than 0.");
        if (totalQty <= 0) errors.add("Total Quantity Packed must be greater than 0.");
        if (pallets <= 0) errors.add("No of Pallets must be greater than 0.");

        ShippingScenario scenario = parseScenario(request.shippingScenario());
        String packingList = resolvePackingList(request.packingListOverride(), pkgId, invoice, packingListSource);
        String revision = !clean(request.revisionLevel()).isBlank()
                ? clean(request.revisionLevel())
                : clean(defaultRevisionLevel);

        if (packingList.isBlank()) {
            errors.add("Packing List # is required for the Master Label. Enter it in Edit or provide a valid source.");
        } else if (packingList.length() > 8) {
            errors.add("Packing List # exceeds the PDF limit of 8 data characters (DI 11K not included). Current length: " + packingList.length());
        }

        if (po.length() > 15) {
            errors.add("Purchase Order exceeds the PDF limit of 15 data characters.");
        }
        if (part.length() > 18) {
            errors.add("Part Number exceeds the PDF limit of 18 data characters.");
        }
        if (String.valueOf(totalQty).length() > 5) {
            warnings.add("Quantity exceeds the 5-digit Master Label data limit.");
        }
        if (perPallet > 0 && pallets > 0) {
            long expected = (long) perPallet * pallets;
            if (expected != totalQty) {
                warnings.add("Quantity mismatch: Per Pallet Qty x No. of Pallets = " + expected + ", but Total Quantity Packed = " + totalQty + ".");
            }
        }

        if (pallets > MAX_PALLETS) {
            errors.add("No of Pallets exceeds the maximum of " + MAX_PALLETS + " per row.");
            pallets = MAX_PALLETS; // keeps the arithmetic below safe; the row is already an error
        }

        int labelUnits = pallets;
        int copiesPerUnit = defaultCopies(scenario);
        int recommended = labelUnits * copiesPerUnit;
        int requested = request.requestedPrintQuantity() == null || request.requestedPrintQuantity() <= 0
                ? recommended
                : request.requestedPrintQuantity();

        if (requested > MAX_PRINT_QUANTITY) {
            errors.add("Requested print quantity exceeds the maximum of " + MAX_PRINT_QUANTITY + " per row.");
        }
        if (requested < labelUnits) {
            errors.add("Requested print quantity must be at least " + labelUnits + " so every pallet/container can receive a Master Label.");
        }
        if (requested != recommended) {
            warnings.add("Print quantity differs from the B-10 rule calculation of " + recommended + " physical labels (" + labelUnits + " units x " + copiesPerUnit + " copies).");
        }
        if (labelUnits > 0 && requested % labelUnits != 0) {
            warnings.add("Requested print quantity is not evenly divisible across label units; copies will be distributed deterministically.");
        }

        String status = errors.isEmpty() ? (warnings.isEmpty() ? "VALID" : "WARNING") : "ERROR";
        List<String> messages = new ArrayList<>(errors);
        messages.addAll(warnings);

        return new ProcessedRowResponse(
                clean(request.clientRowId()), po, invoice, pkgId, item, part, description,
                perPallet, totalQty, pallets, packingList, revision,
                scenario.name(), labelUnits, copiesPerUnit, recommended, requested,
                status, messages
        );
    }

    public String resolvePackingList(String override, String pkgId, String invoice, String source) {
        if (!clean(override).isBlank()) {
            return clean(override);
        }
        String mode = clean(source).toUpperCase();
        return switch (mode) {
            case "PKG_ID" -> clean(pkgId);
            case "INVOICE_NUMBER" -> clean(invoice);
            default -> !clean(pkgId).isBlank() ? clean(pkgId) : clean(invoice);
        };
    }

    public ShippingScenario parseScenario(String scenario) {
        try {
            return ShippingScenario.valueOf(clean(scenario).isBlank()
                    ? ShippingScenario.PALLET_IDENTICAL.name()
                    : clean(scenario).toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ShippingScenario.PALLET_IDENTICAL;
        }
    }

    public int defaultCopies(ShippingScenario scenario) {
        return switch (scenario) {
            case PALLET_MIXED -> 1;
            case PALLET_IDENTICAL, LARGE_CONTAINER_IDENTICAL, LOOSE_CONTAINER -> 2;
        };
    }

    private int safeInt(Integer value) {
        return value == null ? 0 : value;
    }

    private String clean(String value) {
        return value == null ? "" : value.trim();
    }
}
