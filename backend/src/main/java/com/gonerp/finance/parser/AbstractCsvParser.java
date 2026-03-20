package com.gonerp.finance.parser;

import java.io.*;
import java.nio.charset.Charset;
import java.util.*;

public abstract class AbstractCsvParser implements CsvParser {

    protected abstract String getDelimiter();
    protected abstract String getEncoding();

    @Override
    public ParseResult parse(InputStream inputStream, int skipRows) throws Exception {
        Charset charset = Charset.forName(getEncoding());
        BufferedReader reader = new BufferedReader(new InputStreamReader(inputStream, charset));

        List<String> warnings = new ArrayList<>();
        List<String> headers = null;
        List<Map<String, String>> rows = new ArrayList<>();

        // Skip rows
        for (int i = 0; i < skipRows; i++) {
            String line = reader.readLine();
            if (line == null) break;
        }

        String line;
        int lineNum = 0;
        while ((line = reader.readLine()) != null) {
            line = line.trim();
            if (line.isEmpty()) continue;

            List<String> fields = parseLine(line, getDelimiter());

            if (headers == null) {
                headers = fields.stream().map(String::trim).toList();
                continue;
            }

            lineNum++;
            Map<String, String> row = new LinkedHashMap<>();
            for (int i = 0; i < headers.size(); i++) {
                String value = i < fields.size() ? fields.get(i).trim() : "";
                row.put(headers.get(i), value);
            }
            // Include any extra columns beyond headers
            for (int i = headers.size(); i < fields.size(); i++) {
                String extraHeader = "Column_" + (i + 1);
                row.put(extraHeader, fields.get(i).trim());
                if (lineNum == 1) {
                    warnings.add("Extra column found at index " + i);
                }
            }
            rows.add(row);
        }

        if (headers == null) {
            headers = Collections.emptyList();
            warnings.add("No header row found");
        }

        return ParseResult.builder()
                .headers(headers)
                .rows(rows)
                .warnings(warnings)
                .build();
    }

    protected List<String> parseLine(String line, String delimiter) {
        List<String> fields = new ArrayList<>();
        boolean inQuotes = false;
        StringBuilder current = new StringBuilder();

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (c == '"') {
                if (inQuotes && i + 1 < line.length() && line.charAt(i + 1) == '"') {
                    current.append('"');
                    i++;
                } else {
                    inQuotes = !inQuotes;
                }
            } else if (!inQuotes && line.substring(i).startsWith(delimiter)) {
                fields.add(current.toString());
                current = new StringBuilder();
                i += delimiter.length() - 1;
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString());
        return fields;
    }
}
