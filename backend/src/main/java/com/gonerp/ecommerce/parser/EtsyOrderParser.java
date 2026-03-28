package com.gonerp.ecommerce.parser;

import org.springframework.stereotype.Component;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.util.*;

/**
 * Parses Etsy CSV exports (EtsySoldOrders and EtsySoldOrderItems).
 * Handles RFC 4180 CSV properly, including multi-line quoted fields
 * (the Variations column in Items CSV contains newlines within quotes).
 */
@Component
public class EtsyOrderParser {

    /**
     * Parse EtsySoldOrders CSV.
     * Comma delimited, double-quoted, date format MM/DD/YY.
     */
    public List<Map<String, String>> parseOrders(InputStream inputStream) throws IOException {
        return parseCsv(inputStream);
    }

    /**
     * Parse EtsySoldOrderItems CSV.
     * Comma delimited, double-quoted, date format MM/DD/YY for sale date.
     * CRITICAL: Variations field may contain newlines within quoted fields.
     */
    public List<Map<String, String>> parseOrderItems(InputStream inputStream) throws IOException {
        return parseCsv(inputStream);
    }

    /**
     * RFC 4180 compliant CSV parser that handles multi-line quoted fields.
     * Reads char by char, tracking quote state to properly handle newlines inside quotes.
     */
    private List<Map<String, String>> parseCsv(InputStream inputStream) throws IOException {
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8));
        List<Map<String, String>> rows = new ArrayList<>();
        List<String> headers = null;

        List<List<String>> allRecords = readAllRecords(reader);
        for (List<String> fields : allRecords) {
            if (headers == null) {
                headers = new ArrayList<>();
                for (String f : fields) {
                    headers.add(decodeHtmlEntities(f.trim()));
                }
                continue;
            }

            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String value = i < fields.size() ? decodeHtmlEntities(fields.get(i).trim()) : "";
                row.put(headers.get(i), value);
            }
            // Include extra columns beyond headers
            for (int i = headers.size(); i < fields.size(); i++) {
                row.put("Column_" + (i + 1), decodeHtmlEntities(fields.get(i).trim()));
            }
            rows.add(row);
        }

        return rows;
    }

    /**
     * Read all CSV records handling multi-line quoted fields.
     * Implements RFC 4180 parsing: reads char by char, tracks whether we are
     * inside a quoted field, and only treats newlines as record delimiters
     * when we are NOT inside quotes.
     */
    private List<List<String>> readAllRecords(BufferedReader reader) throws IOException {
        List<List<String>> records = new ArrayList<>();
        List<String> currentFields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;
        boolean fieldStarted = false;

        int ch;
        while ((ch = reader.read()) != -1) {
            char c = (char) ch;

            if (inQuotes) {
                if (c == '"') {
                    // Peek next char
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') {
                        // Escaped quote ""
                        currentField.append('"');
                    } else {
                        // End of quoted field
                        inQuotes = false;
                        if (next != -1) {
                            reader.reset();
                        }
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"' && !fieldStarted) {
                    inQuotes = true;
                    fieldStarted = true;
                } else if (c == ',') {
                    currentFields.add(currentField.toString());
                    currentField = new StringBuilder();
                    fieldStarted = false;
                } else if (c == '\n') {
                    currentFields.add(currentField.toString());
                    // Skip empty lines (but not records with empty fields)
                    if (currentFields.size() > 1 || !currentFields.get(0).trim().isEmpty()) {
                        records.add(currentFields);
                    }
                    currentFields = new ArrayList<>();
                    currentField = new StringBuilder();
                    fieldStarted = false;
                } else if (c == '\r') {
                    // Skip \r, will be handled by \n
                } else {
                    currentField.append(c);
                    fieldStarted = true;
                }
            }
        }

        // Handle last field/record
        if (currentField.length() > 0 || !currentFields.isEmpty()) {
            currentFields.add(currentField.toString());
            if (currentFields.size() > 1 || !currentFields.get(0).trim().isEmpty()) {
                records.add(currentFields);
            }
        }

        return records;
    }

    /**
     * Decode common HTML entities found in Etsy CSV exports.
     */
    private String decodeHtmlEntities(String value) {
        if (value == null || value.isEmpty()) return value;
        return value
                .replace("&quot;", "\"")
                .replace("&amp;", "&")
                .replace("&lt;", "<")
                .replace("&gt;", ">")
                .replace("&apos;", "'")
                .replace("&#39;", "'");
    }
}
