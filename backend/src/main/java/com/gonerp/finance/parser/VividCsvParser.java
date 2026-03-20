package com.gonerp.finance.parser;

import com.gonerp.finance.model.enums.AccountType;
import org.springframework.stereotype.Component;

/**
 * Vivid bank uses same format as Wise main account:
 * Comma-delimited, UTF-8, columns: Completed date, Counterparty name, Reference, Payment amount, Payment currency
 */
@Component
public class VividCsvParser extends AbstractCsvParser {

    @Override
    public AccountType getAccountType() {
        return AccountType.VIVID;
    }

    @Override
    protected String getDelimiter() {
        return ",";
    }

    @Override
    protected String getEncoding() {
        return "UTF-8";
    }
}
