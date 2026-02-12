package com.fvjapps.allowancecalculator.misc;

import android.annotation.SuppressLint;
import android.os.Build;

import java.text.SimpleDateFormat;
import java.time.Instant;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Date;

public class MillisConv {
    public static String toDate(long millis, DateFormat format) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            Instant instant = Instant.ofEpochMilli(millis);
            ZonedDateTime zonedDateTime = instant.atZone(ZoneId.systemDefault());
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern(format.getPattern());
            return zonedDateTime.format(formatter);
        } else {
            Date date = new Date(millis);
            @SuppressLint("SimpleDateFormat") SimpleDateFormat sdf = new SimpleDateFormat(format.getPattern());
            return sdf.format(date);
        }
    }

    public enum DateFormat {
        DATABASE_STANDARD("yyyy-MM-dd HH:mm:ss.SSS"),
        FILENAME_SAFE("yyyy-MM-dd_HH-mm-ss-SSS"),
        FILENAME_COMPACT("yyyyMMdd_HHmmss_SSS"),
        FILE_BACKUP("yyyy-MM-dd_HH-mm-ss");

        private final String pattern;

        DateFormat(String pattern) {
            this.pattern = pattern;
        }

        public String getPattern() {
            return pattern;
        }
    }
}
