package com.fvjapps.allowancecalculator.misc;

import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class ExportFileNameTest {
    @Test
    public void includesLedgerNameAndExtensionWithoutPathSeparators() {
        String fileName = ExportFileName.forLedger("../Family Budget", "CSV");

        assertTrue(fileName.endsWith("_.._Family_Budget.csv"));
        assertFalse(fileName.contains("/"));
        assertFalse(fileName.contains("\\"));
    }

    @Test(expected = IllegalArgumentException.class)
    public void rejectsBlankLedgerName() {
        ExportFileName.forLedger("  ", "pdf");
    }
}
