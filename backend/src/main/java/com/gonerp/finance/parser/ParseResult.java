package com.gonerp.finance.parser;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ParseResult {
    private List<String> headers;
    @Builder.Default
    private List<Map<String, String>> rows = new ArrayList<>();
    @Builder.Default
    private List<String> warnings = new ArrayList<>();
}
