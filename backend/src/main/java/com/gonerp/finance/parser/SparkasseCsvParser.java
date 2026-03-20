package com.gonerp.finance.parser;

import com.gonerp.finance.model.enums.AccountType;
import org.springframework.stereotype.Component;

@Component
public class SparkasseCsvParser extends AbstractCsvParser {

    @Override
    public AccountType getAccountType() {
        return AccountType.SPARKASSE;
    }

    @Override
    protected String getDelimiter() {
        return ";";
    }

    @Override
    protected String getEncoding() {
        return "ISO-8859-1";
    }
}
