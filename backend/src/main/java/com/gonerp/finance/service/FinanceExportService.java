package com.gonerp.finance.service;

import com.gonerp.finance.model.FinanceTransaction;
import com.gonerp.finance.model.FinanceTransactionFile;
import com.gonerp.finance.repository.FinanceTransactionFileRepository;
import com.gonerp.finance.repository.FinanceTransactionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceExportService {

    private final FinanceTransactionRepository transactionRepository;
    private final FinanceTransactionFileRepository fileRepository;
    private final FinanceAccessService financeAccessService;

    public byte[] exportReportCsv(Long reportId, Long accountId) {
        financeAccessService.requireFinanceAccess();

        List<FinanceTransaction> transactions;
        if (accountId != null) {
            transactions = transactionRepository.findByMonthlyReportIdAndAccountIdOrderByRowIndexAsc(reportId, accountId);
        } else {
            transactions = transactionRepository.findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId);
        }

        if (transactions.isEmpty()) {
            return "No transactions found\n".getBytes(StandardCharsets.UTF_8);
        }

        // Determine column order from first transaction's file headers
        List<String> originalHeaders = new ArrayList<>();
        if (!transactions.isEmpty()) {
            FinanceTransactionFile file = transactions.get(0).getTransactionFile();
            if (file.getColumnHeaders() != null) {
                originalHeaders.addAll(file.getColumnHeaders());
            } else if (transactions.get(0).getRawData() != null) {
                originalHeaders.addAll(transactions.get(0).getRawData().keySet());
            }
        }

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        PrintWriter writer = new PrintWriter(new OutputStreamWriter(baos, StandardCharsets.UTF_8));

        // Header row: original columns + Note + Category + Subcategory
        List<String> allHeaders = new ArrayList<>(originalHeaders);
        allHeaders.add("Note");
        allHeaders.add("Category");
        allHeaders.add("Subcategory");

        writer.println(String.join(";", allHeaders.stream().map(this::csvEscape).toList()));

        // Data rows
        for (FinanceTransaction tx : transactions) {
            List<String> values = new ArrayList<>();
            Map<String, String> rawData = tx.getRawData() != null ? tx.getRawData() : Collections.emptyMap();

            for (String header : originalHeaders) {
                values.add(csvEscape(rawData.getOrDefault(header, "")));
            }
            values.add(csvEscape(tx.getNote() != null ? tx.getNote() : ""));
            values.add(csvEscape(tx.getCategory() != null ? tx.getCategory() : ""));
            values.add(csvEscape(tx.getSubcategory() != null ? tx.getSubcategory() : ""));

            writer.println(String.join(";", values));
        }

        writer.flush();
        return baos.toByteArray();
    }

    public byte[] exportReportZip(Long reportId) {
        financeAccessService.requireFinanceAccess();

        List<FinanceTransactionFile> files = fileRepository.findByMonthlyReportId(reportId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            for (FinanceTransactionFile file : files) {
                String filename = file.getAccount().getName() + "_" + file.getOriginalFilename();
                filename = filename.replaceAll("[^a-zA-Z0-9._-]", "_");

                byte[] csv = exportReportCsv(reportId, file.getAccount().getId());
                zos.putNextEntry(new ZipEntry(filename.replace(".csv", "_annotated.csv")));
                zos.write(csv);
                zos.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ZIP: " + e.getMessage(), e);
        }

        return baos.toByteArray();
    }

    private String csvEscape(String value) {
        if (value == null) return "";
        if (value.contains(";") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }
}
