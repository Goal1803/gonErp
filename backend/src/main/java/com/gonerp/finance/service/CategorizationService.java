package com.gonerp.finance.service;

import com.gonerp.common.OrgContext;
import com.gonerp.finance.dto.CategorizationRuleRequest;
import com.gonerp.finance.dto.CategorizationRuleResponse;
import com.gonerp.finance.model.FinanceCategorizationRule;
import com.gonerp.finance.model.FinanceMonthlyReport;
import com.gonerp.finance.model.FinanceTransaction;
import com.gonerp.finance.model.enums.AccountType;
import com.gonerp.finance.model.enums.FinanceRole;
import com.gonerp.finance.model.enums.MatchType;
import com.gonerp.finance.repository.FinanceCategorizationRuleRepository;
import com.gonerp.finance.repository.FinanceMonthlyReportRepository;
import com.gonerp.finance.repository.FinanceTransactionRepository;
import com.gonerp.organization.model.Organization;
import com.gonerp.organization.repository.OrganizationRepository;
import com.gonerp.usermanager.repository.UserRepository;
import jakarta.persistence.EntityNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
@Transactional
public class CategorizationService {

    private final FinanceCategorizationRuleRepository ruleRepository;
    private final FinanceTransactionRepository transactionRepository;
    private final FinanceMonthlyReportRepository reportRepository;
    private final FinanceAccessService financeAccessService;
    private final UserRepository userRepository;

    // --- CRUD for rules ---

    public List<CategorizationRuleResponse> findAllRules() {
        financeAccessService.requireFinanceAccess();
        Organization org = financeAccessService.resolveOrganization();
        return ruleRepository.findByOrganizationIdOrderByPriorityAsc(org.getId()).stream()
                .map(CategorizationRuleResponse::from).toList();
    }

    public CategorizationRuleResponse createRule(CategorizationRuleRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();

        FinanceCategorizationRule rule = FinanceCategorizationRule.builder()
                .organization(org)
                .name(request.getName())
                .priority(request.getPriority() != null ? request.getPriority() : 100)
                .active(request.getActive() != null ? request.getActive() : true)
                .accountType(request.getAccountType() != null ? AccountType.valueOf(request.getAccountType()) : null)
                .fieldName(request.getFieldName())
                .matchType(MatchType.valueOf(request.getMatchType()))
                .matchValue(request.getMatchValue())
                .additionalConditions(request.getAdditionalConditions())
                .category(request.getCategory())
                .subcategory(request.getSubcategory())
                .noteTemplate(request.getNoteTemplate())
                .build();

        return CategorizationRuleResponse.from(ruleRepository.save(rule));
    }

    public CategorizationRuleResponse updateRule(Long id, CategorizationRuleRequest request) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();
        FinanceCategorizationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));
        if (!rule.getOrganization().getId().equals(org.getId())) throw new SecurityException("Access denied");

        rule.setName(request.getName());
        if (request.getPriority() != null) rule.setPriority(request.getPriority());
        if (request.getActive() != null) rule.setActive(request.getActive());
        rule.setAccountType(request.getAccountType() != null ? AccountType.valueOf(request.getAccountType()) : null);
        rule.setFieldName(request.getFieldName());
        rule.setMatchType(MatchType.valueOf(request.getMatchType()));
        rule.setMatchValue(request.getMatchValue());
        rule.setAdditionalConditions(request.getAdditionalConditions());
        rule.setCategory(request.getCategory());
        rule.setSubcategory(request.getSubcategory());
        rule.setNoteTemplate(request.getNoteTemplate());

        return CategorizationRuleResponse.from(ruleRepository.save(rule));
    }

    public void deleteRule(Long id) {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();
        FinanceCategorizationRule rule = ruleRepository.findById(id)
                .orElseThrow(() -> new EntityNotFoundException("Rule not found: " + id));
        if (!rule.getOrganization().getId().equals(org.getId())) throw new SecurityException("Access denied");
        ruleRepository.delete(rule);
    }

    // --- Categorization engine ---

    public int categorizeReport(Long reportId) {
        financeAccessService.requireFinanceAccess();

        FinanceMonthlyReport report = reportRepository.findById(reportId)
                .orElseThrow(() -> new EntityNotFoundException("Report not found: " + reportId));
        Organization org = financeAccessService.getOrganizationOr(report.getOrganization());

        List<FinanceCategorizationRule> rules = ruleRepository
                .findByOrganizationIdAndActiveTrueOrderByPriorityAsc(org.getId());
        List<FinanceTransaction> transactions = transactionRepository
                .findByMonthlyReportIdOrderByAccountIdAscRowIndexAsc(reportId);

        int categorized = 0;
        for (FinanceTransaction tx : transactions) {
            if (tx.isManuallyReviewed()) continue;

            for (FinanceCategorizationRule rule : rules) {
                if (matchesRule(tx, rule)) {
                    tx.setCategory(rule.getCategory());
                    tx.setSubcategory(rule.getSubcategory());
                    tx.setNote(applyNoteTemplate(rule.getNoteTemplate(), tx));
                    tx.setAutoCategorized(true);
                    tx.setCompleted(true);
                    categorized++;
                    break; // First match wins
                }
            }
        }

        transactionRepository.saveAll(transactions);
        return categorized;
    }

    private boolean matchesRule(FinanceTransaction tx, FinanceCategorizationRule rule) {
        // Check account type filter
        if (rule.getAccountType() != null && tx.getAccount().getAccountType() != rule.getAccountType()) {
            return false;
        }

        String fieldValue = getFieldValue(tx, rule.getFieldName());
        if (fieldValue == null) return false;

        // Check additional conditions (e.g. amount_gt, amount_lt)
        if (rule.getAdditionalConditions() != null && !rule.getAdditionalConditions().isEmpty()) {
            if (!checkAdditionalConditions(tx, rule.getAdditionalConditions())) {
                return false;
            }
        }

        return switch (rule.getMatchType()) {
            case CONTAINS -> fieldValue.toLowerCase().contains(rule.getMatchValue().toLowerCase());
            case EXACT -> fieldValue.equalsIgnoreCase(rule.getMatchValue());
            case REGEX -> Pattern.compile(rule.getMatchValue(), Pattern.CASE_INSENSITIVE).matcher(fieldValue).find();
            case GT -> {
                try {
                    yield new BigDecimal(fieldValue).compareTo(new BigDecimal(rule.getMatchValue())) > 0;
                } catch (Exception e) { yield false; }
            }
            case LT -> {
                try {
                    yield new BigDecimal(fieldValue).compareTo(new BigDecimal(rule.getMatchValue())) < 0;
                } catch (Exception e) { yield false; }
            }
        };
    }

    private String getFieldValue(FinanceTransaction tx, String fieldName) {
        return switch (fieldName) {
            case "counterparty" -> tx.getCounterparty();
            case "description" -> tx.getDescription();
            case "amount" -> tx.getAmount() != null ? tx.getAmount().toPlainString() : null;
            case "currency" -> tx.getCurrency();
            default -> {
                // Try raw_data
                if (tx.getRawData() != null) {
                    yield tx.getRawData().get(fieldName);
                }
                yield null;
            }
        };
    }

    // --- Seed default rules ---

    public int seedDefaultRules() {
        financeAccessService.requireFinanceRole(FinanceRole.FINANCE_CFO, FinanceRole.FINANCE_ACCOUNTANT_MANAGER);
        Organization org = financeAccessService.resolveOrganization();

        List<FinanceCategorizationRule> existing = ruleRepository.findByOrganizationIdOrderByPriorityAsc(org.getId());
        if (!existing.isEmpty()) {
            return 0; // Don't overwrite existing rules
        }

        String[][] defaults = {
            // priority, fieldName, matchType, matchValue, category, subcategory, noteTemplate
            // --- Internal transfers ---
            {"10", "counterparty", "CONTAINS", "GON Commerce Supply", "Internal Transfer", "Inter-Account", "Transfer: {{counterparty}}"},
            {"11", "counterparty", "CONTAINS", "GON COMMERCE SUPPLY", "Internal Transfer", "Inter-Account", "Transfer: {{counterparty}}"},
            {"12", "description", "CONTAINS", "internal transfer", "Internal Transfer", "Inter-Account", ""},
            {"13", "description", "CONTAINS", "internal/", "Internal Transfer", "Inter-Account", ""},
            // --- Marketplace income ---
            {"20", "counterparty", "CONTAINS", "AMAZON PAYMENTS", "Sales Income", "Amazon", "Amazon payout"},
            {"21", "counterparty", "CONTAINS", "Amazon", "Sales Income", "Amazon", "Amazon: {{description}}"},
            {"22", "counterparty", "CONTAINS", "Etsy Payments", "Sales Income", "Etsy", "Etsy payout"},
            {"23", "counterparty", "CONTAINS", "Etsy.com", "Sales Income", "Etsy", "Etsy: {{description}}"},
            // --- Suppliers / COGS ---
            {"30", "counterparty", "CONTAINS", "Printify", "COGS", "Printing/Fulfillment", "Printify: {{description}}"},
            {"31", "counterparty", "CONTAINS", "PRINTWAY", "COGS", "Printing/Fulfillment", "Printway"},
            {"32", "counterparty", "CONTAINS", "BurgerPrints", "COGS", "Printing/Fulfillment", "BurgerPrints"},
            {"33", "counterparty", "CONTAINS", "Yjl", "COGS", "Product Purchase", "Yjl supplier payment"},
            // --- Software & subscriptions ---
            {"40", "counterparty", "CONTAINS", "Shopify", "Operating Expenses", "Software", "Shopify subscription"},
            {"41", "counterparty", "CONTAINS", "CLAUDE.AI", "Operating Expenses", "Software", "Claude AI subscription"},
            {"42", "counterparty", "CONTAINS", "lexoffice", "Operating Expenses", "Accounting Software", "lexoffice"},
            {"43", "counterparty", "CONTAINS", "TRELLO", "Operating Expenses", "Software", "Trello/Atlassian"},
            {"44", "counterparty", "CONTAINS", "Google Payment", "Operating Expenses", "Software", "Google: {{description}}"},
            {"45", "counterparty", "CONTAINS", "Namecheap", "Operating Expenses", "Domain/Hosting", "Namecheap: {{description}}"},
            // --- Telecom ---
            {"50", "counterparty", "CONTAINS", "Drillisch", "Operating Expenses", "Telecom", "Mobile: {{description}}"},
            // --- Banking fees ---
            {"60", "description", "CONTAINS", "Entgeltabrechnung", "Bank Fees", "Account Fees", "Sparkasse account fee"},
            {"61", "counterparty", "CONTAINS", "Vivid Money", "Bank Fees", "Account Fees", "Vivid account fee"},
            // --- Rent ---
            {"70", "description", "REGEX", "(?i)miete", "Operating Expenses", "Rent", "Rent payment"},
            // --- Tax ---
            {"80", "counterparty", "CONTAINS", "Finanzamt", "Tax", "VAT/USt", "Tax office: {{description}}"},
            // --- Amazon specific ---
            {"90", "description", "CONTAINS", "Cost of Advertising", "Marketing", "Amazon Ads", "Amazon advertising"},
            {"91", "description", "CONTAINS", "FBA storage", "Operating Expenses", "Amazon FBA", "FBA storage fee"},
            {"92", "description", "CONTAINS", "Service Fees", "Operating Expenses", "Amazon Fees", "Amazon service fee"},
            {"93", "description", "CONTAINS", "Subscription", "Operating Expenses", "Amazon Fees", "Amazon seller subscription"},
            // --- Payoneer ---
            {"100", "counterparty", "CONTAINS", "WISE", "Internal Transfer", "Payoneer-Wise", "Wise to Payoneer transfer"},
            // --- Currency conversion ---
            {"110", "description", "CONTAINS", "umgetauscht", "Internal Transfer", "Currency Conversion", "Currency conversion"},
            {"111", "description", "CONTAINS", "Currency Conversion", "Internal Transfer", "Currency Conversion", "Currency conversion"},
            // --- Shipping ---
            {"120", "counterparty", "CONTAINS", "Deutsche Post", "Operating Expenses", "Shipping", "Deutsche Post: {{description}}"},
            // --- PayPal specific types ---
            {"130", "description", "CONTAINS", "Bank Deposit to PP", "Internal Transfer", "PayPal Funding", "PayPal bank deposit"},
            {"131", "description", "CONTAINS", "Debit Card Cash Back", "Other Income", "Cashback", "PayPal cashback"},
            {"132", "description", "CONTAINS", "Account Hold", "Internal Transfer", "PayPal Hold", "PayPal hold"},
            {"133", "description", "CONTAINS", "Reversal of General Account Hold", "Internal Transfer", "PayPal Hold", "PayPal hold reversal"},
        };

        int count = 0;
        for (String[] d : defaults) {
            FinanceCategorizationRule rule = FinanceCategorizationRule.builder()
                    .organization(org)
                    .name(d[4] + " - " + d[3])
                    .priority(Integer.parseInt(d[0]))
                    .active(true)
                    .fieldName(d[1])
                    .matchType(MatchType.valueOf(d[2]))
                    .matchValue(d[3])
                    .category(d[4])
                    .subcategory(d[5])
                    .noteTemplate(d[6])
                    .build();
            ruleRepository.save(rule);
            count++;
        }
        return count;
    }

    private boolean checkAdditionalConditions(FinanceTransaction tx, Map<String, Object> conditions) {
        BigDecimal amount = tx.getAmount();
        if (amount == null) amount = BigDecimal.ZERO;

        Object amountGt = conditions.get("amount_gt");
        if (amountGt != null) {
            try {
                if (amount.compareTo(new BigDecimal(amountGt.toString())) <= 0) return false;
            } catch (Exception e) { return false; }
        }

        Object amountLt = conditions.get("amount_lt");
        if (amountLt != null) {
            try {
                if (amount.compareTo(new BigDecimal(amountLt.toString())) >= 0) return false;
            } catch (Exception e) { return false; }
        }

        return true;
    }

    private String applyNoteTemplate(String template, FinanceTransaction tx) {
        if (template == null || template.isBlank()) return null;
        String result = template;
        result = result.replace("{{counterparty}}", tx.getCounterparty() != null ? tx.getCounterparty() : "");
        result = result.replace("{{amount}}", tx.getAmount() != null ? tx.getAmount().toPlainString() : "");
        result = result.replace("{{description}}", tx.getDescription() != null ? tx.getDescription() : "");
        result = result.replace("{{currency}}", tx.getCurrency() != null ? tx.getCurrency() : "");
        return result;
    }
}
