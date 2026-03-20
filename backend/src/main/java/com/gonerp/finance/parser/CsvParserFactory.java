package com.gonerp.finance.parser;

import com.gonerp.finance.model.enums.AccountType;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Component
public class CsvParserFactory {

    private final Map<AccountType, CsvParser> parsers;

    public CsvParserFactory(List<CsvParser> parserList) {
        this.parsers = parserList.stream()
                .collect(Collectors.toMap(CsvParser::getAccountType, Function.identity()));
    }

    public CsvParser getParser(AccountType accountType) {
        CsvParser parser = parsers.get(accountType);
        if (parser == null) {
            throw new IllegalArgumentException("No CSV parser found for account type: " + accountType);
        }
        return parser;
    }
}
