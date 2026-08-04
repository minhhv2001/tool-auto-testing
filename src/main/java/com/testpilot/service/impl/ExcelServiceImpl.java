package com.testpilot.service.impl;

import com.testpilot.model.entity.TestStep;
import com.testpilot.model.enums.ActionType;
import com.testpilot.model.response.ImportResult;
import com.testpilot.service.ExcelService;
import org.apache.poi.ss.usermodel.*;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.*;

public final class ExcelServiceImpl implements ExcelService {
    public static final String AUTOMATION_SHEET = "Automation Steps";
    private static final Set<String> REQUIRED = Set.of("testcaseid", "step", "action");

    @Override
    public ImportResult importAutomationSteps(Path file) {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File Excel khong ton tai");
        }
        List<String> warnings = new ArrayList<>();
        List<TestStep> steps = new ArrayList<>();
        try (InputStream input = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet(AUTOMATION_SHEET);
            if (sheet == null) {
                throw new IllegalArgumentException("File can co sheet '" + AUTOMATION_SHEET + "'");
            }
            Row header = firstNonEmptyRow(sheet);
            if (header == null) throw new IllegalArgumentException("Sheet Automation Steps dang trong");
            Map<String, Integer> columns = headerMap(header);
            if (!columns.keySet().containsAll(REQUIRED)) {
                throw new IllegalArgumentException("Thieu cot bat buoc: TestCaseID, Step hoac Action");
            }
            DataFormatter formatter = new DataFormatter();
            for (int index = header.getRowNum() + 1; index <= sheet.getLastRowNum(); index++) {
                Row row = sheet.getRow(index);
                if (row == null || isBlank(row, formatter)) continue;
                try {
                    String testCaseId = cell(row, columns, "testcaseid", formatter);
                    int stepNumber = parseInt(cell(row, columns, "step", formatter), index);
                    ActionType action = ActionType.fromCell(cell(row, columns, "action", formatter));
                    int timeout = parseIntOrDefault(cell(row, columns, "timeoutms", formatter), 15_000);
                    boolean enabled = parseEnabled(cell(row, columns, "enabled", formatter));
                    steps.add(new TestStep(testCaseId, stepNumber,
                            cell(row, columns, "description", formatter), action,
                            cell(row, columns, "target", formatter),
                            cell(row, columns, "input", formatter),
                            cell(row, columns, "expected", formatter), timeout, enabled));
                } catch (RuntimeException error) {
                    throw new IllegalArgumentException("Dong Excel " + (index + 1) + ": " + error.getMessage(), error);
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("Khong doc duoc file Excel", error);
        }
        steps.sort(Comparator.comparing(TestStep::testCaseId).thenComparingInt(TestStep::stepNumber));
        if (steps.isEmpty()) throw new IllegalArgumentException("Khong co buoc test hop le");
        if (steps.stream().noneMatch(TestStep::enabled)) warnings.add("Tat ca cac buoc dang bi tat");
        return new ImportResult(List.copyOf(steps), List.copyOf(warnings));
    }

    private static Row firstNonEmptyRow(Sheet sheet) {
        for (Row row : sheet) {
            if (row != null && row.cellIterator().hasNext()) return row;
        }
        return null;
    }

    private static Map<String, Integer> headerMap(Row row) {
        DataFormatter formatter = new DataFormatter();
        Map<String, Integer> result = new HashMap<>();
        for (Cell cell : row) {
            String key = normalize(formatter.formatCellValue(cell));
            if (!key.isBlank()) result.put(key, cell.getColumnIndex());
        }
        return result;
    }

    private static String normalize(String value) {
        return value == null ? "" : value.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String cell(Row row, Map<String, Integer> columns, String name, DataFormatter formatter) {
        Integer index = columns.get(name);
        if (index == null) return "";
        Cell cell = row.getCell(index, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
        return cell == null ? "" : formatter.formatCellValue(cell).trim();
    }

    private static boolean isBlank(Row row, DataFormatter formatter) {
        for (Cell cell : row) if (!formatter.formatCellValue(cell).isBlank()) return false;
        return true;
    }

    private static int parseInt(String value, int rowIndex) {
        if (value == null || value.isBlank()) return rowIndex;
        return (int) Double.parseDouble(value.replace(',', '.'));
    }

    private static int parseIntOrDefault(String value, int fallback) {
        try {
            return value == null || value.isBlank() ? fallback : (int) Double.parseDouble(value.replace(',', '.'));
        } catch (NumberFormatException ignored) {
            return fallback;
        }
    }

    private static boolean parseEnabled(String value) {
        if (value == null || value.isBlank()) return true;
        return !Set.of("false", "0", "no", "n", "khong").contains(value.toLowerCase(Locale.ROOT));
    }
}
