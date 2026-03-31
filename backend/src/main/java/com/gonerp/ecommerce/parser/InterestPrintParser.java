package com.gonerp.ecommerce.parser;

import com.gonerp.ecommerce.model.EcomSupplier;
import com.gonerp.ecommerce.model.EcomSupplierTransaction;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Parser for InterestPrint supplier files (XML-based .xls format).
 */
@Component
public class InterestPrintParser implements SupplierFileParser {

    private static final Pattern DATA_PATTERN = Pattern.compile("<Data[^>]*>(.*?)</Data>");
    private static final Pattern ROW_PATTERN = Pattern.compile("<Row>(.*?)</Row>", Pattern.DOTALL);

    @Override
    public String getSupplierName() {
        return "Interest Print";
    }

    @Override
    public List<EcomSupplierTransaction> parse(EcomSupplier supplier, MultipartFile file) throws Exception {
        String content = new String(file.getBytes(), "UTF-8");
        List<Map<String, String>> rawRows = parseXlsXml(content);
        List<EcomSupplierTransaction> result = new ArrayList<>();

        for (Map<String, String> row : rawRows) {
            String orderId = cleanStr(row.get("Order ID"));
            if (orderId == null) continue;

            result.add(EcomSupplierTransaction.builder()
                    .supplier(supplier)
                    .supplierOrderId(orderId)
                    .orderDate(parseDateTime(row.get("Date")))
                    .amount(parseDollarAmount(row.get("Amount")))
                    .currency("USD")
                    .quantity(parseIntSafe(row.get("Qty."), 1))
                    .status(cleanStr(row.get("Status")))
                    .fullName(cleanStr(row.get("Full Name")))
                    .country(cleanStr(row.get("Country")))
                    .streetAddress(cleanStr(row.get("Street Address")))
                    .city(cleanStr(row.get("City")))
                    .stateRegion(cleanStr(row.get("State/Region")))
                    .postalCode(cleanStr(row.get("Postal Code")))
                    .phoneNumber(cleanStr(row.get("Phone Number")))
                    .shipMethod(cleanStr(row.get("Ship method")))
                    .trackingId(cleanStr(row.get("Tracking ID")))
                    .shipDate(parseDateTime(row.get("Ship date")))
                    .sku(cleanStr(row.get("SKU")))
                    .size(cleanStr(row.get("Size")))
                    .build());
        }
        return result;
    }

    private List<Map<String, String>> parseXlsXml(String content) {
        List<Map<String, String>> result = new ArrayList<>();
        Matcher rowMatcher = ROW_PATTERN.matcher(content);
        List<String> headers = null;

        while (rowMatcher.find()) {
            String rowContent = rowMatcher.group(1);
            Matcher cellMatcher = DATA_PATTERN.matcher(rowContent);
            List<String> values = new ArrayList<>();
            while (cellMatcher.find()) {
                String val = cellMatcher.group(1)
                        .replace("&amp;", "&").replace("&lt;", "<")
                        .replace("&gt;", ">").replace("&quot;", "\"");
                values.add(val);
            }
            if (headers == null) {
                headers = values;
            } else {
                Map<String, String> row = new LinkedHashMap<>();
                for (int i = 0; i < headers.size() && i < values.size(); i++) {
                    row.put(headers.get(i), values.get(i));
                }
                result.add(row);
            }
        }
        return result;
    }

    private BigDecimal parseDollarAmount(String s) {
        if (s == null || s.isBlank() || s.equals("--")) return null;
        String cleaned = s.replaceAll("[^\\d.,-]", "");
        if (cleaned.isEmpty()) return null;
        try { return new BigDecimal(cleaned); }
        catch (NumberFormatException e) { return null; }
    }

    private LocalDateTime parseDateTime(String s) {
        if (s == null || s.isBlank() || s.startsWith("0000")) return null;
        try { return LocalDateTime.parse(s, DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")); }
        catch (Exception e) { return null; }
    }

    private String cleanStr(String s) {
        if (s == null || s.isBlank()) return null;
        return s.trim();
    }

    private int parseIntSafe(String s, int def) {
        if (s == null || s.isBlank()) return def;
        try { return Integer.parseInt(s.trim()); }
        catch (NumberFormatException e) { return def; }
    }
}
