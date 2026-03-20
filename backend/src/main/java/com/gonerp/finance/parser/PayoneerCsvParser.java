package com.gonerp.finance.parser;

import com.gonerp.finance.model.enums.AccountType;
import org.springframework.stereotype.Component;

@Component
public class PayoneerCsvParser extends AbstractCsvParser {

    @Override
    public AccountType getAccountType() {
        return AccountType.PAYONEER;
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
