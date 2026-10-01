package com.fvjapps.allowancecalculator.misc;

import androidx.annotation.NonNull;

import java.util.Locale;

public final class ExportFileName {
    private ExportFileName() {
    }

    @NonNull
    public static String forLedger(String ledgerName, String extension) {
        if (ledgerName == null || ledgerName.isBlank()) {
            throw new IllegalArgumentException("Ledger name must not be blank");
        }
        if (extension == null || !extension.matches("[a-zA-Z0-9]+")) {
            throw new IllegalArgumentException("Invalid file extension");
        }
        String sanitizedName = ledgerName.trim()
                .replaceAll("[\\\\/:*?\"<>|\\p{Cntrl}]", "_")
                .replaceAll("\\s+", "_")
                .replaceAll("_+", "_");
        return String.format(
                Locale.ROOT,
                "%s_%s.%s",
                MillisConv.toDate(
                        System.currentTimeMillis(),
                        MillisConv.DateFormat.FILE_BACKUP
                ),
                sanitizedName,
                extension.toLowerCase(Locale.ROOT)
        );
    }
}
