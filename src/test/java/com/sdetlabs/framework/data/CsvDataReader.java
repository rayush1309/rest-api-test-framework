package com.sdetlabs.framework.data;

import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVParser;
import org.apache.commons.csv.CSVRecord;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/** Reads header-based CSV fixtures, including fields containing quoted commas. */
public final class CsvDataReader {
    private CsvDataReader() { }

    public static List<Map<String, String>> read(String resourcePath) throws IOException {
        InputStream input = CsvDataReader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IllegalArgumentException("Test resource not found on classpath: " + resourcePath);
        }
        try (InputStream stream = input;
             Reader reader = new InputStreamReader(stream, StandardCharsets.UTF_8);
             CSVParser parser = CSVFormat.RFC4180.builder()
                     .setHeader()
                     .setSkipHeaderRecord(true)
                     .setIgnoreEmptyLines(true)
                     .get()
                     .parse(reader)) {
            List<Map<String, String>> rows = new ArrayList<>();
            for (CSVRecord record : parser) rows.add(record.toMap());
            return List.copyOf(rows);
        }
    }
}
