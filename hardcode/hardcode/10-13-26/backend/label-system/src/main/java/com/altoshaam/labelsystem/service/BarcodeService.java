package com.altoshaam.labelsystem.service;

import org.springframework.stereotype.Service;

import com.google.zxing.oned.Code128Writer;

@Service
public class BarcodeService {

    /**
     * Encodes the data as Code 128 and returns the bar pattern, one entry per
     * module (true = black bar). Used to draw crisp vector barcodes in the PDF.
     * A new writer is created per call, so this method is thread-safe.
     */
    public boolean[] encodeCode128(String data) {
        if (data == null || data.isBlank()) {
            throw new IllegalArgumentException("Barcode data cannot be blank.");
        }
        return new Code128Writer().encode(sanitize(data));
    }

    public String sanitize(String value) {
        if (value == null) {
            return "";
        }
        return value
                .replace('\u2013', '-')
                .replace('\u2014', '-')
                .replace('\u2018', '\'')
                .replace('\u2019', '\'')
                .replace('\u201c', '"')
                .replace('\u201d', '"')
                .replace('\u00A0', ' ')
                .replaceAll("[^\\x20-\\x7E]", "?");
    }
}
