package com.altoshaam.labelsystem.service;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class SerialNumberFormatTest {

    @Test
    void expectedMasterSerialShape() {
        String supplierId = "12345";
        String sequence = String.format("%010d", 1);
        String serial = supplierId + sequence;
        assertEquals(15, serial.length());
        assertEquals("9S" + serial, "9S123450000000001");
    }
}
