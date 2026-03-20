package com.gonerp.finance.parser;

import com.gonerp.finance.model.enums.AccountType;

import java.io.InputStream;

public interface CsvParser {

    AccountType getAccountType();

    ParseResult parse(InputStream inputStream, int skipRows) throws Exception;
}
