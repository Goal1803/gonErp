package com.gonerp.finance.service;

import com.gonerp.common.FileStorageService;
import com.gonerp.finance.dto.TransactionFileResponse;
import com.gonerp.finance.model.*;
import com.gonerp.finance.model.enums.AccountType;
import com.gonerp.finance.model.enums.ParseStatus;
import com.gonerp.finance.model.enums.UploadFileType;
import com.gonerp.finance.parser.CsvParserFactory;
import com.gonerp.finance.parser.ParseResult;
import com.gonerp.finance.repository.FinanceAccountRepository;
import com.gonerp.finance.repository.FinanceMonthlyReportRepository;
import com.gonerp.finance.repository.FinanceTransactionFileRepository;
import com.gonerp.finance.repository.FinanceTransactionRepository;
import com.gonerp.organization.model.Organization;

import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.*;

@Service
@RequiredArgsConstructor
@Transactional
public class TransactionFileService {

    private final FinanceTransactionFileRepository fileRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final FinanceAccountRepository accountRepository;
    private final FinanceMonthlyReportRepository reportRepository;
    private final FinanceAccessService financeAccessService;
    private final CsvParserFactory csvParserFactory;
    private final FileStorageService fileStorageService;

    public List<TransactionFileResponse> findByReport(Long reportId) {
        financeAccessService.requireFinanceAccess();
        return fileRepository.findByMonthlyReportId(reportId).stream()
                .map(TransactionFileResponse::from)
                .toList();
    }

    public TransactionFileResponse upload(Long reportId, Long accountId, String fileType, String subfolder, MultipartFile file) {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();

        FinanceMonthlyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found: " + reportId));
        FinanceAccount account = accountRepository.findById(accountId)
                .orElseThrow(() -> new EntityNotFoundException("Account not found: " + accountId));

        UploadFileType uploadType = fileType != null ? UploadFileType.valueOf(fileType) : UploadFileType.TRANSACTION;

        // Enforce: only 1 transaction file per account per report
        if (uploadType == UploadFileType.TRANSACTION &&
                fileRepository.existsByMonthlyReportIdAndAccountIdAndFileType(reportId, accountId, UploadFileType.TRANSACTION)) {
            throw new IllegalArgumentException("A transaction file already exists for this account in this report. Delete the existing one first.");
        }

        String storageUrl;
        try {
            String folder = "finance/" + org.getId() + "/" + report.getYear() + "/" + report.getMonth();
            storageUrl = fileStorageService.store(file, folder);
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file: " + e.getMessage(), e);
        }

        FinanceTransactionFile txFile = FinanceTransactionFile.builder()
                .organization(org)
                .account(account)
                .monthlyReport(report)
                .originalFilename(file.getOriginalFilename())
                .storageUrl(storageUrl)
                .parseStatus(uploadType == UploadFileType.TRANSACTION ? ParseStatus.PENDING : ParseStatus.SUCCESS)
                .fileType(uploadType)
                .subfolder(subfolder != null && !subfolder.isBlank() ? subfolder.trim() : null)
                .build();
        txFile = fileRepository.save(txFile);

        // Only parse transaction files
        if (uploadType != UploadFileType.TRANSACTION) {
            return TransactionFileResponse.from(txFile);
        }

        try {
            var parser = csvParserFactory.getParser(account.getAccountType());
            ParseResult result = parser.parse(file.getInputStream(), account.getCsvSkipRows());

            txFile.setColumnHeaders(result.getHeaders());
            txFile.setRowCount(result.getRows().size());
            txFile.setParseStatus(ParseStatus.SUCCESS);

            List<FinanceTransaction> transactions = new ArrayList<>();
            for (int i = 0; i < result.getRows().size(); i++) {
                Map<String, String> row = result.getRows().get(i);
                FinanceTransaction tx = FinanceTransaction.builder()
                        .organization(org)
                        .transactionFile(txFile)
                        .account(account)
                        .monthlyReport(report)
                        .rawData(row)
                        .rowIndex(i)
                        .build();

                extractCanonicalFields(tx, row, account.getAccountType(), account);
                transactions.add(tx);
            }

            transactionRepository.saveAll(transactions);
            fileRepository.save(txFile);

        } catch (Exception e) {
            txFile.setParseStatus(ParseStatus.FAILED);
            txFile.setParseError(e.getMessage());
            fileRepository.save(txFile);
        }

        return TransactionFileResponse.from(txFile);
    }

    public TransactionFileResponse moveToSubfolder(Long fileId, String subfolder) {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        FinanceTransactionFile txFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + fileId));
        if (!txFile.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        txFile.setSubfolder(subfolder != null && !subfolder.isBlank() ? subfolder.trim() : null);
        return TransactionFileResponse.from(fileRepository.save(txFile));
    }

    public int renameSubfolder(Long reportId, Long accountId, String oldName, String newName) {
        financeAccessService.requireFinanceAccess();
        List<FinanceTransactionFile> files = fileRepository.findByMonthlyReportIdAndAccountId(reportId, accountId);
        int count = 0;
        for (FinanceTransactionFile f : files) {
            if (f.getSubfolder() != null) {
                if (f.getSubfolder().equals(oldName) || f.getSubfolder().startsWith(oldName + "/")) {
                    f.setSubfolder(newName + f.getSubfolder().substring(oldName.length()));
                    fileRepository.save(f);
                    count++;
                }
            }
        }
        return count;
    }

    /**
     * Re-extract canonical fields from raw_data for all transactions in a report.
     * Useful after parser logic is updated.
     */
    public int reExtractReport(Long reportId) {
        financeAccessService.requireFinanceAccess();
        List<FinanceTransaction> transactions = transactionRepository
                .findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId);
        int count = 0;
        for (FinanceTransaction tx : transactions) {
            if (tx.getRawData() != null) {
                extractCanonicalFields(tx, tx.getRawData(), tx.getAccount().getAccountType(), tx.getAccount());
                count++;
            }
        }
        transactionRepository.saveAll(transactions);
        return count;
    }

    public void delete(Long fileId) {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        FinanceTransactionFile txFile = fileRepository.findById(fileId)
                .orElseThrow(() -> new EntityNotFoundException("File not found: " + fileId));
        if (!txFile.getOrganization().getId().equals(org.getId())) {
            throw new SecurityException("Access denied");
        }
        transactionRepository.deleteByTransactionFileId(fileId);
        fileRepository.delete(txFile);
    }

    /**
     * Extracts canonical fields based on known column layouts per account type.
     * Each account type has hardcoded knowledge of its CSV column names.
     * Falls back to account.columnMapping if set, for custom/future bank types.
     */
    private void extractCanonicalFields(FinanceTransaction tx, Map<String, String> row,
                                         AccountType accountType, FinanceAccount account) {
        try {
            switch (accountType) {
                case SPARKASSE -> extractSparkasse(tx, row);
                case PAYPAL -> extractPayPal(tx, row);
                case WISE_MAIN -> extractWise(tx, row);
                case WISE_SUB -> extractWise(tx, row);
                case VIVID -> extractVivid(tx, row);
                case PAYONEER -> extractPayoneer(tx, row);
                case AMAZON_SELLER -> extractAmazonSeller(tx, row);
                case AMAZON_VAT -> extractAmazonVat(tx, row);
                case CASH -> extractCash(tx, row, account);
            }
        } catch (Exception ignored) {
            // Best effort - don't fail the whole parse
        }
    }

    // --- Sparkasse: semicolon, ISO-8859-1 ---
    // Columns: Auftragskonto;Buchungstag;Valutadatum;Buchungstext;Verwendungszweck;...;Beguenstigter/Zahlungspflichtiger;...;Betrag;Waehrung;Info
    private void extractSparkasse(FinanceTransaction tx, Map<String, String> row) {
        tx.setTransactionDate(parseDate(row.get("Buchungstag"), "dd.MM.yy", "dd.MM.yyyy"));
        String counterparty = row.get("Beguenstigter/Zahlungspflichtiger");
        tx.setCounterparty(counterparty);
        tx.setDescription(row.get("Verwendungszweck"));
        tx.setAmount(parseGermanAmount(row.get("Betrag")));
        tx.setCurrency(getOrDefault(row.get("Waehrung"), "EUR"));

        // Inter-account transfer detection: marketplace incoming payments
        if (counterparty != null) {
            String upper = counterparty.toUpperCase();
            if (upper.contains("AMAZON PAYMENTS") || upper.contains("ETSY PAYMENTS")) {
                tx.setInterAccount(true);
            }
        }
    }

    // --- PayPal: comma, UTF-8, 41 columns ---
    // Columns: Date,Time,TimeZone,Name,Type,Status,Currency,Gross,Fee,Net,...,Balance,...
    private void extractPayPal(FinanceTransaction tx, Map<String, String> row) {
        tx.setTransactionDate(parseDate(row.get("Date"), "MM/dd/yyyy", "dd/MM/yyyy", "dd.MM.yyyy"));
        tx.setCounterparty(row.get("Name"));
        tx.setDescription(row.get("Type"));
        tx.setAmount(parseGermanAmount(row.get("Gross")));
        tx.setCurrency(getOrDefault(row.get("Currency"), "EUR"));
        tx.setBalanceAfter(parseGermanAmount(row.get("Balance")));
    }

    // --- Wise (both EUR and USD accounts): comma, UTF-8 ---
    // Columns: TransferWise ID,Date,Date Time,Amount,Currency,Description,Payment Reference,
    //   Running Balance,Exchange From,Exchange To,Exchange Rate,Payer Name,Payee Name,
    //   Payee Account Number,Merchant,Card Last Four Digits,Card Holder Full Name,
    //   Attachment,Note,Total fees,Exchange To Amount,Transaction Type,Transaction Details Type
    private void extractWise(FinanceTransaction tx, Map<String, String> row) {
        tx.setTransactionDate(parseDate(row.get("Date"), "dd-MM-yyyy", "yyyy-MM-dd"));
        tx.setDescription(row.get("Description"));
        tx.setAmount(parseStandardAmount(row.get("Amount")));
        tx.setCurrency(getOrDefault(row.get("Currency"), "EUR"));
        tx.setBalanceAfter(parseStandardAmount(row.get("Running Balance")));

        // Determine counterparty based on transaction type
        String txType = row.get("Transaction Type");
        String detailsType = row.get("Transaction Details Type");
        String payerName = row.get("Payer Name");
        String payeeName = row.get("Payee Name");
        String merchant = row.get("Merchant");

        if ("DEBIT".equals(txType)) {
            tx.setCounterparty(payeeName != null && !payeeName.isBlank() ? payeeName
                    : merchant != null && !merchant.isBlank() ? merchant : null);
        } else if ("CREDIT".equals(txType)) {
            tx.setCounterparty(payerName != null && !payerName.isBlank() ? payerName : null);
        }

        // Add payment reference to description if present
        String paymentRef = row.get("Payment Reference");
        if (paymentRef != null && !paymentRef.isBlank()) {
            String desc = tx.getDescription();
            tx.setDescription((desc != null && !desc.isBlank() ? desc + " | " : "") + paymentRef);
        }

        // Inter-account transfer detection
        if ("DEPOSIT".equals(detailsType) && payerName != null
                && payerName.toUpperCase().contains("GON COMMERCE")) {
            tx.setInterAccount(true);
        } else if ("TRANSFER".equals(detailsType) && payeeName != null
                && payeeName.toUpperCase().contains("GON COMMERCE")) {
            tx.setInterAccount(true);
        } else if ("CONVERSION".equals(detailsType)) {
            tx.setInterAccount(true);
        }
    }

    // --- Vivid (Statement files): comma, UTF-8 ---
    // Columns: Completed date,Counterparty name,Reference,Payment amount,Payment currency
    private void extractVivid(FinanceTransaction tx, Map<String, String> row) {
        tx.setTransactionDate(parseDate(row.get("Completed date"), "dd.MM.yyyy", "yyyy-MM-dd"));
        tx.setCounterparty(row.get("Counterparty name"));
        tx.setDescription(row.get("Reference"));
        tx.setAmount(parseStandardAmount(row.get("Payment amount")));
        tx.setCurrency(getOrDefault(row.get("Payment currency"), "EUR"));

        // Inter-account transfer detection
        String reference = row.get("Reference");
        if (reference != null && reference.toLowerCase().contains("internal")) {
            tx.setInterAccount(true);
        }
    }

    // --- Payoneer: comma, UTF-8 ---
    // Columns: Currency,Payout method,Transaction date,Transaction time,Time zone,Credit amount,Debit amount,Status,Running balance,Description
    private void extractPayoneer(FinanceTransaction tx, Map<String, String> row) {
        tx.setTransactionDate(parseDate(row.get("Transaction date"), "MM-dd-yyyy", "dd-MM-yyyy"));
        tx.setDescription(row.get("Description"));
        tx.setCurrency(getOrDefault(row.get("Currency"), "USD"));
        tx.setBalanceAfter(parseStandardAmount(row.get("Running balance")));

        BigDecimal credit = parseStandardAmount(row.get("Credit amount"));
        BigDecimal debit = parseStandardAmount(row.get("Debit amount"));
        if (credit != null && credit.signum() > 0) {
            tx.setAmount(credit);
        } else if (debit != null && debit.signum() > 0) {
            tx.setAmount(debit.negate());
        } else {
            tx.setAmount(credit != null ? credit : debit);
        }

        // Try to extract counterparty from description
        String desc = row.get("Description");
        if (desc != null) {
            if (desc.startsWith("Payment to ")) tx.setCounterparty(desc.substring(11));
            else if (desc.startsWith("Payment from ")) tx.setCounterparty(desc.substring(13));
            else tx.setCounterparty(desc);
        }
    }

    // --- Amazon Seller: comma, UTF-8 ---
    // Columns: Date,Transaction status,Transaction type,Order ID,Product Details,Total product charges,Total promotional rebates,Amazon fees,Other,Total (Currency)
    private void extractAmazonSeller(FinanceTransaction tx, Map<String, String> row) {
        tx.setTransactionDate(parseDate(row.get("Date"), "dd/MM/yyyy", "MM/dd/yyyy"));
        tx.setCounterparty("Amazon");
        String type = row.get("Transaction type");
        String orderId = row.get("Order ID");
        String product = row.get("Product Details");
        tx.setDescription(type + (orderId != null && !orderId.isBlank() ? " | " + orderId : "")
                + (product != null && !product.isBlank() ? " | " + product : ""));

        // Total column may include currency like "Total (EUR)" or "Total (GBP)"
        String totalValue = null;
        String detectedCurrency = null;
        for (Map.Entry<String, String> entry : row.entrySet()) {
            if (entry.getKey().startsWith("Total")) {
                totalValue = entry.getValue();
                // Extract currency from column name: "Total (EUR)" -> "EUR"
                int paren = entry.getKey().indexOf('(');
                if (paren > 0) {
                    detectedCurrency = entry.getKey().substring(paren + 1).replace(")", "").trim();
                }
                break;
            }
        }
        tx.setAmount(parseStandardAmount(totalValue));
        tx.setCurrency(detectedCurrency != null ? detectedCurrency : "EUR");
    }

    // --- Amazon VAT: comma, UTF-8, 70+ columns ---
    private void extractAmazonVat(FinanceTransaction tx, Map<String, String> row) {
        tx.setTransactionDate(parseDate(row.get("TAX_CALCULATION_DATE"), "yyyy-MM-dd", "dd/MM/yyyy"));
        tx.setCounterparty(getOrDefault(row.get("MARKETPLACE"), "Amazon"));
        tx.setDescription(row.get("TRANSACTION_TYPE") + " | " + getOrDefault(row.get("ITEM_DESCRIPTION"), ""));
        tx.setAmount(parseStandardAmount(row.get("TOTAL_ACTIVITY_VALUE_AMT_VAT_INCL")));
        tx.setCurrency(getOrDefault(row.get("TRANSACTION_CURRENCY_CODE"), "EUR"));
    }

    // --- Cash: flexible, uses account.columnMapping or simple defaults ---
    private void extractCash(FinanceTransaction tx, Map<String, String> row, FinanceAccount account) {
        Map<String, Object> mapping = account.getColumnMapping();
        if (mapping != null) {
            extractWithMapping(tx, row, mapping, account);
        }
        // Cash defaults to account currency
        if (tx.getCurrency() == null) tx.setCurrency(account.getCurrency());
    }

    private void extractWithMapping(FinanceTransaction tx, Map<String, String> row,
                                     Map<String, Object> mapping, FinanceAccount account) {
        String dateCol = (String) mapping.get("transaction_date");
        if (dateCol != null && row.containsKey(dateCol)) {
            tx.setTransactionDate(parseDate(row.get(dateCol), "dd.MM.yyyy", "MM/dd/yyyy", "yyyy-MM-dd"));
        }
        String cpCol = (String) mapping.get("counterparty");
        if (cpCol != null && row.containsKey(cpCol)) tx.setCounterparty(row.get(cpCol));

        String descCol = (String) mapping.get("description");
        if (descCol != null && row.containsKey(descCol)) tx.setDescription(row.get(descCol));

        String amtCol = (String) mapping.get("amount");
        if (amtCol != null && row.containsKey(amtCol)) tx.setAmount(parseGermanAmount(row.get(amtCol)));

        String curCol = (String) mapping.get("currency");
        if (curCol != null && row.containsKey(curCol)) tx.setCurrency(row.get(curCol));
        else tx.setCurrency(account.getCurrency());
    }

    // --- Parsing Helpers ---

    private LocalDate parseDate(String dateStr, String... patterns) {
        if (dateStr == null || dateStr.isBlank()) return null;
        String trimmed = dateStr.trim();
        // Handle datetime strings by taking date part only
        if (trimmed.contains(" ")) trimmed = trimmed.split(" ")[0];

        for (String pattern : patterns) {
            try {
                return LocalDate.parse(trimmed, DateTimeFormatter.ofPattern(pattern));
            } catch (Exception ignored) {}
        }
        return null;
    }

    /** Parse German number format: 1.234,56 → 1234.56 */
    private BigDecimal parseGermanAmount(String amtStr) {
        if (amtStr == null || amtStr.isBlank()) return null;
        String cleaned = amtStr.trim().replaceAll("[^\\d.,-]", "");
        if (cleaned.isEmpty()) return null;

        // German: dots as thousands sep, comma as decimal
        if (cleaned.contains(",") && (cleaned.indexOf(',') > cleaned.lastIndexOf('.') || !cleaned.contains("."))) {
            cleaned = cleaned.replace(".", "").replace(",", ".");
        }
        try { return new BigDecimal(cleaned); }
        catch (NumberFormatException e) { return null; }
    }

    /** Parse standard number: 1,234.56 or plain 1234.56 or negative -1234.56 */
    private BigDecimal parseStandardAmount(String amtStr) {
        if (amtStr == null || amtStr.isBlank()) return null;
        String cleaned = amtStr.trim().replaceAll("[^\\d.,-]", "");
        if (cleaned.isEmpty()) return null;

        // If both , and . present, and comma comes first, it's a thousands separator
        if (cleaned.contains(",") && cleaned.contains(".") && cleaned.indexOf(',') < cleaned.lastIndexOf('.')) {
            cleaned = cleaned.replace(",", "");
        }
        // If only comma, treat as decimal
        else if (cleaned.contains(",") && !cleaned.contains(".")) {
            cleaned = cleaned.replace(",", ".");
        }
        // If comma after dot (German format)
        else if (cleaned.contains(",") && cleaned.indexOf(',') > cleaned.lastIndexOf('.')) {
            cleaned = cleaned.replace(".", "").replace(",", ".");
        }

        try { return new BigDecimal(cleaned); }
        catch (NumberFormatException e) { return null; }
    }

    private String getOrDefault(String value, String defaultValue) {
        return (value != null && !value.isBlank()) ? value.trim() : defaultValue;
    }
}
