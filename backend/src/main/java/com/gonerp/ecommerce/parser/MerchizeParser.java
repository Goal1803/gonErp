package com.gonerp.ecommerce.parser;

import com.gonerp.ecommerce.model.EcomSupplier;
import com.gonerp.ecommerce.model.EcomSupplierTransaction;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Parser for Merchize supplier CSV exports.
 * Each CSV row is one line-item; rows sharing the same "Number" are aggregated
 * into a single EcomSupplierTransaction (amount = sum of Fulfillment cost,
 * quantity = sum of Item Quantity). Other fields are taken from the first row.
 */
@Component
public class MerchizeParser implements SupplierFileParser {

    @Override
    public String getSupplierName() {
        return "Merchize";
    }

    @Override
    public List<EcomSupplierTransaction> parse(EcomSupplier supplier, MultipartFile file) throws Exception {
        List<Map<String, String>> rawRows = parseCsv(file);

        // Group by order Number — preserve insertion order
        Map<String, List<Map<String, String>>> grouped = new LinkedHashMap<>();
        for (Map<String, String> row : rawRows) {
            String number = cleanStr(row.get("Number"));
            if (number == null) continue;
            grouped.computeIfAbsent(number, k -> new ArrayList<>()).add(row);
        }

        List<EcomSupplierTransaction> result = new ArrayList<>();
        for (Map.Entry<String, List<Map<String, String>>> entry : grouped.entrySet()) {
            String number = entry.getKey();
            List<Map<String, String>> rows = entry.getValue();
            Map<String, String> first = rows.get(0);

            BigDecimal fulfillmentTotal = BigDecimal.ZERO;
            int qtyTotal = 0;
            for (Map<String, String> r : rows) {
                BigDecimal cost = parseAmount(r.get("Fulfillment cost"));
                if (cost != null) fulfillmentTotal = fulfillmentTotal.add(cost);
                qtyTotal += parseIntSafe(r.get("Item Quantity"), 0);
            }
            if (qtyTotal == 0) qtyTotal = parseIntSafe(first.get("Quantity"), 1);

            String streetAddress = combineStreet(
                    cleanStr(first.get("Shipping address line 1")),
                    cleanStr(first.get("Shipping address line 2")));

            String shipMethod = cleanStr(first.get("Tracking Company"));
            if (shipMethod == null) shipMethod = cleanStr(first.get("Shipping method"));

            String currency = cleanStr(first.get("Currency"));
            if (currency == null) currency = "USD";

            result.add(EcomSupplierTransaction.builder()
                    .supplier(supplier)
                    .supplierOrderId(number)
                    .externalNumber(cleanStr(first.get("External number")))
                    .orderDate(parseDateTime(first.get("Created at")))
                    .amount(fulfillmentTotal.signum() == 0 ? null : fulfillmentTotal)
                    .currency(currency)
                    .quantity(qtyTotal)
                    .status(cleanStr(first.get("Fulfillment status")))
                    .fullName(cleanStr(first.get("Shipping name")))
                    .country(cleanStr(first.get("Shipping Country")))
                    .streetAddress(streetAddress)
                    .city(cleanStr(first.get("Shipping city")))
                    .stateRegion(cleanStr(first.get("Shipping Province")))
                    .postalCode(cleanStr(first.get("Shipping Zip")))
                    .phoneNumber(cleanStr(first.get("Phone number")))
                    .shipMethod(shipMethod)
                    .trackingId(cleanStr(first.get("Tracking Number")))
                    .shipDate(parseDateTime(first.get("Shipped at")))
                    .sku(cleanStr(first.get("SKU")))
                    .size(cleanStr(first.get("Size")))
                    .build());
        }
        return result;
    }

    /**
     * RFC 4180-ish CSV reader. Handles quoted fields with embedded commas and newlines.
     * First record is treated as the header row; remaining records are mapped by header name.
     */
    private List<Map<String, String>> parseCsv(MultipartFile file) throws Exception {
        List<List<String>> records;
        try (BufferedReader reader = new BufferedReader(
                new InputStreamReader(file.getInputStream(), StandardCharsets.UTF_8))) {
            records = readAllRecords(reader);
        }
        if (records.isEmpty()) return Collections.emptyList();

        List<String> headers = records.get(0).stream().map(h -> h.trim()).toList();
        List<Map<String, String>> rows = new ArrayList<>();
        for (int i = 1; i < records.size(); i++) {
            List<String> fields = records.get(i);
            Map<String, String> row = new LinkedHashMap<>();
            for (int j = 0; j < headers.size(); j++) {
                row.put(headers.get(j), j < fields.size() ? fields.get(j) : "");
            }
            rows.add(row);
        }
        return rows;
    }

    private List<List<String>> readAllRecords(BufferedReader reader) throws Exception {
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
                    reader.mark(1);
                    int next = reader.read();
                    if (next == '"') {
                        currentField.append('"');
                    } else {
                        inQuotes = false;
                        if (next != -1) reader.reset();
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
                    if (currentFields.size() > 1 || !currentFields.get(0).trim().isEmpty()) {
                        records.add(currentFields);
                    }
                    currentFields = new ArrayList<>();
                    currentField = new StringBuilder();
                    fieldStarted = false;
                } else if (c != '\r') {
                    currentField.append(c);
                    fieldStarted = true;
                }
            }
        }
        if (currentField.length() > 0 || !currentFields.isEmpty()) {
            currentFields.add(currentField.toString());
            if (currentFields.size() > 1 || !currentFields.get(0).trim().isEmpty()) {
                records.add(currentFields);
            }
        }
        return records;
    }

    private String combineStreet(String s1, String s2) {
        if (s1 == null) return s2;
        if (s2 == null) return s1;
        return s1 + " " + s2;
    }

    /**
     * Merchize date format: "yyyy-MM-dd HH:mm +HH:mm". We drop the offset and
     * treat the value as a naive LocalDateTime (matches existing supplier txn semantics).
     */
    private LocalDateTime parseDateTime(String s) {
        String v = cleanStr(s);
        if (v == null) return null;
        // strip trailing " +HH:mm" / " -HH:mm" offset if present
        int spaceIdx = v.lastIndexOf(' ');
        if (spaceIdx > 0) {
            String tail = v.substring(spaceIdx + 1);
            if (tail.matches("[+-]\\d{2}:?\\d{2}")) {
                v = v.substring(0, spaceIdx);
            }
        }
        String[] patterns = {"yyyy-MM-dd HH:mm", "yyyy-MM-dd HH:mm:ss"};
        for (String p : patterns) {
            try { return LocalDateTime.parse(v, DateTimeFormatter.ofPattern(p)); }
            catch (Exception ignored) {}
        }
        return null;
    }

    private BigDecimal parseAmount(String s) {
        String v = cleanStr(s);
        if (v == null || v.equals("--")) return null;
        String cleaned = v.replaceAll("[^\\d.,-]", "");
        if (cleaned.isEmpty()) return null;
        if (cleaned.contains(",") && cleaned.contains(".") && cleaned.indexOf(',') < cleaned.lastIndexOf('.')) {
            cleaned = cleaned.replace(",", "");
        } else if (cleaned.contains(",") && !cleaned.contains(".")) {
            cleaned = cleaned.replace(",", ".");
        }
        try { return new BigDecimal(cleaned); }
        catch (NumberFormatException e) { return null; }
    }

    private int parseIntSafe(String s, int def) {
        String v = cleanStr(s);
        if (v == null) return def;
        try { return Integer.parseInt(v); }
        catch (NumberFormatException e) { return def; }
    }

    private String cleanStr(String s) {
        if (s == null) return null;
        String t = s.trim();
        return t.isEmpty() ? null : t;
    }
}
