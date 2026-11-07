package com.sdetlabs.framework.data;

import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.FormulaEvaluator;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

/** Reads the first row as column names and subsequent rows as string-valued records. */
public final class ExcelDataReader {
    private ExcelDataReader() { }

    public static List<Map<String, String>> read(String resourcePath, String sheetName) throws IOException {
        InputStream input = ExcelDataReader.class.getClassLoader().getResourceAsStream(resourcePath);
        if (input == null) {
            throw new IllegalArgumentException("Test resource not found on classpath: " + resourcePath);
        }
        try (InputStream stream = input; Workbook workbook = WorkbookFactory.create(stream)) {
            Sheet sheet = workbook.getSheet(sheetName);
            if (sheet == null) {
                throw new IllegalArgumentException("Sheet '" + sheetName + "' was not found in " + resourcePath);
            }
            Row header = sheet.getRow(sheet.getFirstRowNum());
            if (header == null || header.getLastCellNum() <= 0) {
                throw new IllegalArgumentException("Excel sheet has no header row: " + resourcePath + "!" + sheetName);
            }
            DataFormatter formatter = new DataFormatter(Locale.ROOT);
            FormulaEvaluator evaluator = workbook.getCreationHelper().createFormulaEvaluator();
            List<String> columns = new ArrayList<>();
            for (int column = 0; column < header.getLastCellNum(); column++) {
                String name = format(header.getCell(column), formatter, evaluator).trim();
                if (name.isEmpty()) throw new IllegalArgumentException("Excel header contains a blank column name");
                columns.add(name);
            }

            List<Map<String, String>> rows = new ArrayList<>();
            for (int rowIndex = sheet.getFirstRowNum() + 1; rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || isBlank(row, columns.size(), formatter, evaluator)) continue;
                Map<String, String> record = new LinkedHashMap<>();
                for (int column = 0; column < columns.size(); column++) {
                    record.put(columns.get(column), format(row.getCell(column), formatter, evaluator).trim());
                }
                rows.add(Map.copyOf(record));
            }
            return List.copyOf(rows);
        }
    }

    private static boolean isBlank(Row row, int columnCount, DataFormatter formatter, FormulaEvaluator evaluator) {
        for (int column = 0; column < columnCount; column++) {
            if (!format(row.getCell(column), formatter, evaluator).isBlank()) return false;
        }
        return true;
    }

    private static String format(Cell cell, DataFormatter formatter, FormulaEvaluator evaluator) {
        return cell == null ? "" : formatter.formatCellValue(cell, evaluator);
    }
}
