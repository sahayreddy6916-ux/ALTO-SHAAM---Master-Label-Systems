package com.altoshaam.labelsystem.service;

import com.altoshaam.labelsystem.entity.SerialNumberSequence;
import com.altoshaam.labelsystem.exception.LabelValidationException;
import com.altoshaam.labelsystem.repository.SerialNumberSequenceRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Service
public class SerialNumberService {

    private final SerialNumberSequenceRepository sequenceRepository;

    public SerialNumberService(SerialNumberSequenceRepository sequenceRepository) {
        this.sequenceRepository = sequenceRepository;
    }

    @Transactional
    public List<String> reserveSerialNumbers(String supplierId, int count) {
        String normalizedSupplierId = supplierId == null ? "" : supplierId.trim();
        if (!normalizedSupplierId.matches("\\d{1,14}")) {
            throw new LabelValidationException(List.of(
                    "Supplier ID must contain 1 to 14 numeric digits before labels can be generated."
            ));
        }
        if (count <= 0) {
            return List.of();
        }

        sequenceRepository.ensureSequence(normalizedSupplierId);
        SerialNumberSequence sequence = sequenceRepository.findBySupplierId(normalizedSupplierId)
                .orElseThrow(() -> new IllegalStateException("Unable to initialize serial sequence for supplier " + normalizedSupplierId));

        int uniqueDigits = 15 - normalizedSupplierId.length();
        long maxSequence = pow10(uniqueDigits) - 1;
        long first = sequence.getCurrentSequence() + 1L;
        long last = sequence.getCurrentSequence() + count;

        if (last > maxSequence) {
            throw new LabelValidationException(List.of(
                    "Serial number capacity exceeded for supplier ID " + normalizedSupplierId
                            + ". The 15-digit Master Label serial format cannot hold another sequence range."
            ));
        }

        sequence.setCurrentSequence(last);
        sequence.setUpdatedAt(Instant.now());
        sequenceRepository.save(sequence);

        List<String> serials = new ArrayList<>(count);
        for (long seq = first; seq <= last; seq++) {
            String uniquePart = String.format("%0" + uniqueDigits + "d", seq);
            String serial = normalizedSupplierId + uniquePart;
            if (!serial.matches("\\d{15}")) {
                throw new IllegalStateException("Generated serial is not exactly 15 numeric digits.");
            }
            serials.add(serial);
        }
        return serials;
    }

    private long pow10(int exponent) {
        long result = 1L;
        for (int i = 0; i < exponent; i++) {
            result *= 10L;
        }
        return result;
    }
}
