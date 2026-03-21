package com.gonerp.finance.service;

import com.gonerp.config.R2StorageProperties;
import com.gonerp.finance.model.*;
import com.gonerp.finance.model.enums.ExportStatus;
import com.gonerp.finance.model.enums.UploadFileType;
import com.gonerp.finance.repository.*;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.*;
import java.util.stream.Collectors;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FinanceExportService {

    private static final Logger log = LoggerFactory.getLogger(FinanceExportService.class);

    private final FinanceTransactionRepository transactionRepository;
    private final FinanceTransactionFileRepository fileRepository;
    private final FinanceAccessService financeAccessService;
    private final FinanceReportExportRepository exportRepository;
    private final FinanceInvoiceRepository invoiceRepository;
    private final FinanceAccountRepository accountRepository;
    private final FinanceMonthlyReportRepository monthlyReportRepository;
    private final S3Client s3Client;
    private final R2StorageProperties r2Props;

    // --- Excel export for single account (download button) ---

    public byte[] exportReportExcel(Long reportId, Long accountId) {
        financeAccessService.requireFinanceAccess();

        List<FinanceTransaction> transactions;
        if (accountId != null) {
            transactions = transactionRepository.findByMonthlyReportIdAndAccountIdOrderByRowIndexAsc(reportId, accountId);
        } else {
            transactions = transactionRepository.findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId);
        }

        if (transactions.isEmpty()) {
            return buildEmptyExcel();
        }

        return buildProcessedExcel(transactions, Collections.emptyMap());
    }

    public byte[] exportReportZip(Long reportId) {
        financeAccessService.requireFinanceAccess();
        List<FinanceTransactionFile> files = fileRepository.findByMonthlyReportId(reportId);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {
            Set<Long> doneAccounts = new HashSet<>();
            for (FinanceTransactionFile file : files) {
                Long acctId = file.getAccount().getId();
                if (doneAccounts.contains(acctId)) continue;
                doneAccounts.add(acctId);

                String filename = sanitizeFilename(file.getAccount().getName()) + "_processed.xlsx";
                byte[] xlsx = exportReportExcel(reportId, acctId);
                zos.putNextEntry(new ZipEntry(filename));
                zos.write(xlsx);
                zos.closeEntry();
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create ZIP: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    // --- Full ZIP package generation ---

    @Transactional
    public FinanceReportExport generateReportZip(Long reportId) {
        financeAccessService.requireFinanceAccess();

        FinanceMonthlyReport report = monthlyReportRepository.findById(reportId)
                .orElseThrow(() -> new IllegalArgumentException("Monthly report not found: " + reportId));

        String currentUser = SecurityContextHolder.getContext().getAuthentication().getName();
        String filename = String.format("finance_report_%d_%02d.zip", report.getYear(), report.getMonth());

        FinanceReportExport export = FinanceReportExport.builder()
                .organization(report.getOrganization())
                .monthlyReport(report)
                .filename(filename)
                .status(ExportStatus.GENERATING)
                .generatedBy(currentUser)
                .build();
        export = exportRepository.save(export);

        try {
            byte[] zipBytes = buildFullReportZip(report);

            String key = "finance/exports/" + report.getOrganization().getId()
                    + "/" + report.getYear() + "/" + report.getMonth()
                    + "/" + UUID.randomUUID() + "_" + filename;

            s3Client.putObject(
                    PutObjectRequest.builder()
                            .bucket(r2Props.getBucket())
                            .key(key)
                            .contentType("application/zip")
                            .build(),
                    RequestBody.fromBytes(zipBytes));

            String storageUrl = r2Props.getPublicUrl() + "/" + key;
            export.setStorageUrl(storageUrl);
            export.setFileSize((long) zipBytes.length);
            export.setStatus(ExportStatus.COMPLETED);
            exportRepository.save(export);

        } catch (Exception e) {
            log.error("Failed to generate report ZIP for report {}: {}", reportId, e.getMessage(), e);
            export.setStatus(ExportStatus.FAILED);
            export.setErrorMessage(e.getMessage() != null ? e.getMessage().substring(0, Math.min(e.getMessage().length(), 2000)) : "Unknown error");
            exportRepository.save(export);
        }

        return export;
    }

    private byte[] buildFullReportZip(FinanceMonthlyReport report) {
        Long reportId = report.getId();

        List<FinanceTransaction> allTransactions = transactionRepository.findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId);
        Map<Long, List<FinanceTransaction>> txByAccount = allTransactions.stream()
                .collect(Collectors.groupingBy(tx -> tx.getAccount().getId(), LinkedHashMap::new, Collectors.toList()));

        List<FinanceTransactionFile> allFiles = fileRepository.findByMonthlyReportId(reportId);
        Map<Long, List<FinanceTransactionFile>> filesByAccount = allFiles.stream()
                .collect(Collectors.groupingBy(f -> f.getAccount().getId()));

        List<FinanceInvoice> allInvoices = invoiceRepository.findByMonthlyReportId(reportId);
        Map<Long, List<FinanceInvoice>> invoicesByTransaction = allInvoices.stream()
                .filter(inv -> inv.getTransaction() != null)
                .collect(Collectors.groupingBy(inv -> inv.getTransaction().getId()));

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        try (ZipOutputStream zos = new ZipOutputStream(baos)) {

            for (Map.Entry<Long, List<FinanceTransaction>> entry : txByAccount.entrySet()) {
                Long accountId = entry.getKey();
                List<FinanceTransaction> transactions = entry.getValue();
                if (transactions.isEmpty()) continue;

                String accountName = sanitizeFilename(transactions.get(0).getAccount().getName());
                String folderPrefix = accountName + "/";

                // 1. Processed Excel
                byte[] processedXlsx = buildProcessedExcel(transactions, invoicesByTransaction);
                zos.putNextEntry(new ZipEntry(folderPrefix + accountName + "_processed.xlsx"));
                zos.write(processedXlsx);
                zos.closeEntry();

                // 2. Raw Excel
                byte[] rawXlsx = buildRawExcel(transactions);
                zos.putNextEntry(new ZipEntry(folderPrefix + accountName + "_raw.xlsx"));
                zos.write(rawXlsx);
                zos.closeEntry();

                // 3. Upload files
                List<FinanceTransactionFile> accountFiles = filesByAccount.getOrDefault(accountId, Collections.emptyList());
                for (FinanceTransactionFile file : accountFiles) {
                    if (file.getStorageUrl() == null || file.getStorageUrl().isBlank()) continue;
                    String subfolder = file.getSubfolder() != null && !file.getSubfolder().isBlank()
                            ? file.getSubfolder() + "/" : "";
                    String uploadPath = folderPrefix + "Other_files/" + subfolder + sanitizeFilename(file.getOriginalFilename());
                    byte[] fileBytes = downloadFile(file.getStorageUrl());
                    if (fileBytes != null) {
                        zos.putNextEntry(new ZipEntry(uploadPath));
                        zos.write(fileBytes);
                        zos.closeEntry();
                    }
                }

                // 4. Invoices
                for (FinanceTransaction tx : transactions) {
                    List<FinanceInvoice> txInvoices = invoicesByTransaction.getOrDefault(tx.getId(), Collections.emptyList());
                    for (FinanceInvoice invoice : txInvoices) {
                        if (invoice.getStorageUrl() == null || invoice.getStorageUrl().isBlank()) continue;
                        String counterparty = tx.getCounterparty() != null ? sanitizeFilename(tx.getCounterparty()) : "unknown";
                        String invoiceFilename = "row" + (tx.getRowIndex() + 1) + "_" + counterparty + "_" + sanitizeFilename(invoice.getOriginalFilename());
                        byte[] invoiceBytes = downloadFile(invoice.getStorageUrl());
                        if (invoiceBytes != null) {
                            zos.putNextEntry(new ZipEntry(folderPrefix + "invoices/" + invoiceFilename));
                            zos.write(invoiceBytes);
                            zos.closeEntry();
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new RuntimeException("Failed to create full report ZIP: " + e.getMessage(), e);
        }
        return baos.toByteArray();
    }

    // --- Excel builders ---

    private byte[] buildProcessedExcel(List<FinanceTransaction> transactions,
                                        Map<Long, List<FinanceInvoice>> invoicesByTransaction) {
        if (transactions.isEmpty()) return buildEmptyExcel();

        List<String> originalHeaders = getOriginalHeaders(transactions);

        List<String> allHeaders = new ArrayList<>(originalHeaders);
        allHeaders.add("Note");
        allHeaders.add("Category");
        allHeaders.add("Subcategory");
        if (invoicesByTransaction != null && !invoicesByTransaction.isEmpty()) {
            allHeaders.add("Invoice Files");
        }

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Transactions");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Number style for amounts
            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));

            // Date style
            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));

            // Header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < allHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(allHeaders.get(i));
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            int rowNum = 1;
            for (FinanceTransaction tx : transactions) {
                Row row = sheet.createRow(rowNum++);
                Map<String, String> rawData = tx.getRawData() != null ? tx.getRawData() : Collections.emptyMap();
                int col = 0;

                // Original columns
                for (String header : originalHeaders) {
                    String value = rawData.getOrDefault(header, "");
                    setCellWithType(row.createCell(col), value, numberStyle, dateStyle);
                    col++;
                }

                // Note
                row.createCell(col++).setCellValue(tx.getNote() != null ? tx.getNote() : "");
                // Category
                row.createCell(col++).setCellValue(tx.getCategory() != null ? tx.getCategory() : "");
                // Subcategory
                row.createCell(col++).setCellValue(tx.getSubcategory() != null ? tx.getSubcategory() : "");

                // Invoice Files
                if (invoicesByTransaction != null && !invoicesByTransaction.isEmpty()) {
                    List<FinanceInvoice> txInvoices = invoicesByTransaction.getOrDefault(tx.getId(), Collections.emptyList());
                    String invoiceFileNames = txInvoices.stream()
                            .map(inv -> {
                                String cp = tx.getCounterparty() != null ? sanitizeFilename(tx.getCounterparty()) : "unknown";
                                return "row" + (tx.getRowIndex() + 1) + "_" + cp + "_" + sanitizeFilename(inv.getOriginalFilename());
                            })
                            .collect(Collectors.joining(", "));
                    row.createCell(col).setCellValue(invoiceFileNames);
                }
            }

            // Auto-size columns (limit to avoid slow performance on many columns)
            for (int i = 0; i < Math.min(allHeaders.size(), 20); i++) {
                sheet.autoSizeColumn(i);
                // Cap max width at 50 chars
                if (sheet.getColumnWidth(i) > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build Excel: " + e.getMessage(), e);
        }
    }

    private byte[] buildRawExcel(List<FinanceTransaction> transactions) {
        if (transactions.isEmpty()) return buildEmptyExcel();

        List<String> originalHeaders = getOriginalHeaders(transactions);

        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Raw Data");

            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerStyle.setFont(headerFont);

            CellStyle numberStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            numberStyle.setDataFormat(format.getFormat("#,##0.00"));

            CellStyle dateStyle = workbook.createCellStyle();
            dateStyle.setDataFormat(format.getFormat("dd.mm.yyyy"));

            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < originalHeaders.size(); i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(originalHeaders.get(i));
                cell.setCellStyle(headerStyle);
            }

            int rowNum = 1;
            for (FinanceTransaction tx : transactions) {
                Row row = sheet.createRow(rowNum++);
                Map<String, String> rawData = tx.getRawData() != null ? tx.getRawData() : Collections.emptyMap();
                for (int i = 0; i < originalHeaders.size(); i++) {
                    String value = rawData.getOrDefault(originalHeaders.get(i), "");
                    setCellWithType(row.createCell(i), value, numberStyle, dateStyle);
                }
            }

            for (int i = 0; i < Math.min(originalHeaders.size(), 20); i++) {
                sheet.autoSizeColumn(i);
                if (sheet.getColumnWidth(i) > 50 * 256) {
                    sheet.setColumnWidth(i, 50 * 256);
                }
            }

            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to build raw Excel: " + e.getMessage(), e);
        }
    }

    /**
     * Sets cell value with proper type detection:
     * - Numbers (including German format with comma decimal) → numeric cell
     * - Dates (dd.MM.yyyy, dd/MM/yyyy, yyyy-MM-dd) → text (preserved as-is for clarity)
     * - Everything else → text
     */
    private void setCellWithType(Cell cell, String value, CellStyle numberStyle, CellStyle dateStyle) {
        if (value == null || value.isEmpty()) {
            cell.setCellValue("");
            return;
        }

        String trimmed = value.trim();

        // Try to parse as number
        Double numericValue = tryParseNumber(trimmed);
        if (numericValue != null) {
            cell.setCellValue(numericValue);
            cell.setCellStyle(numberStyle);
            return;
        }

        // Default: text
        cell.setCellValue(trimmed);
    }

    /**
     * Tries to parse a string as a number, handling:
     * - Standard: 1234.56, -1234.56
     * - German: 1.234,56 → 1234.56
     * - German: -4,99 → -4.99
     * - With currency symbols stripped
     */
    private Double tryParseNumber(String s) {
        if (s == null || s.isEmpty()) return null;

        // Strip common non-numeric prefixes/suffixes but keep minus sign
        String cleaned = s.replaceAll("[^\\d.,%+-]", "").trim();
        if (cleaned.isEmpty() || cleaned.equals("-") || cleaned.equals("+")) return null;

        // Must have at least one digit
        if (!cleaned.matches(".*\\d.*")) return null;

        // German format: dots as thousands, comma as decimal (e.g., "1.234,56" or "-4,99")
        if (cleaned.contains(",")) {
            if (cleaned.contains(".") && cleaned.indexOf(',') > cleaned.lastIndexOf('.')) {
                // "1.234,56" → "1234.56"
                cleaned = cleaned.replace(".", "").replace(",", ".");
            } else if (!cleaned.contains(".")) {
                // "-4,99" → "-4.99"
                cleaned = cleaned.replace(",", ".");
            } else if (cleaned.indexOf(',') < cleaned.lastIndexOf('.')) {
                // "1,234.56" → "1234.56" (US format)
                cleaned = cleaned.replace(",", "");
            }
        }

        // Remove percentage sign
        cleaned = cleaned.replace("%", "");

        try {
            return Double.parseDouble(cleaned);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private byte[] buildEmptyExcel() {
        try (XSSFWorkbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Empty");
            Row row = sheet.createRow(0);
            row.createCell(0).setCellValue("No transactions found");
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            workbook.write(baos);
            return baos.toByteArray();
        } catch (Exception e) {
            return new byte[0];
        }
    }

    private List<String> getOriginalHeaders(List<FinanceTransaction> transactions) {
        List<String> headers = new ArrayList<>();
        FinanceTransactionFile file = transactions.get(0).getTransactionFile();
        if (file.getColumnHeaders() != null) {
            headers.addAll(file.getColumnHeaders());
        } else if (transactions.get(0).getRawData() != null) {
            headers.addAll(transactions.get(0).getRawData().keySet());
        }
        return headers;
    }

    // --- List / Delete exports ---

    @Transactional(readOnly = true)
    public List<FinanceReportExport> listExports(Long reportId) {
        financeAccessService.requireFinanceAccess();
        return exportRepository.findByMonthlyReportIdOrderByCreatedAtDesc(reportId);
    }

    @Transactional
    public void deleteExport(Long exportId) {
        financeAccessService.requireFinanceAccess();
        FinanceReportExport export = exportRepository.findById(exportId)
                .orElseThrow(() -> new IllegalArgumentException("Export not found: " + exportId));
        if (export.getStorageUrl() != null && export.getStorageUrl().startsWith(r2Props.getPublicUrl())) {
            String key = export.getStorageUrl().substring(r2Props.getPublicUrl().length() + 1);
            try {
                s3Client.deleteObject(DeleteObjectRequest.builder()
                        .bucket(r2Props.getBucket())
                        .key(key)
                        .build());
            } catch (Exception e) {
                log.warn("Failed to delete R2 object for export {}: {}", exportId, e.getMessage());
            }
        }
        exportRepository.delete(export);
    }

    // --- Helpers ---

    private byte[] downloadFile(String url) {
        try {
            HttpClient httpClient = HttpClient.newHttpClient();
            HttpRequest request = HttpRequest.newBuilder().uri(URI.create(url)).GET().build();
            HttpResponse<byte[]> response = httpClient.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() == 200) return response.body();
            log.warn("Failed to download file {}: status {}", url, response.statusCode());
            return null;
        } catch (Exception e) {
            log.warn("Failed to download file {}: {}", url, e.getMessage());
            return null;
        }
    }

    private String sanitizeFilename(String name) {
        if (name == null) return "unknown";
        return name.replaceAll("[^a-zA-Z0-9._\\-\\s]", "_").replaceAll("\\s+", "_");
    }
}
