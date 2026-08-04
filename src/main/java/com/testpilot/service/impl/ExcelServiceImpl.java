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
        return importAutomationSteps(file, null);
    }

    @Override
    public ImportResult importAutomationSteps(Path file, String requestedSheet) {
        if (file == null || !Files.isRegularFile(file)) {
            throw new IllegalArgumentException("File Excel không tồn tại");
        }
        List<String> warnings = new ArrayList<>();
        List<TestStep> steps = new ArrayList<>();
        try (InputStream input = Files.newInputStream(file); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = requestedSheet == null ? null : findSheet(workbook, requestedSheet);
            if (sheet == null) sheet = workbook.getSheet(AUTOMATION_SHEET);
            if (sheet == null) {
                for (Sheet candidate : workbook) {
                    if (firstNonEmptyRow(candidate) != null) { sheet = candidate; break; }
                }
            }
            if (sheet == null) throw new IllegalArgumentException("File Excel không có sheet dữ liệu");
            Row header = firstNonEmptyRow(sheet);
            if (header == null) throw new IllegalArgumentException("Sheet \"" + sheet.getSheetName() + "\" đang trống");
            Map<String, Integer> columns = headerMap(header);
            if (!columns.keySet().containsAll(REQUIRED)) {
                throw new IllegalArgumentException("Thiếu cột bắt buộc: Mã testcase, Bước hoặc Thao tác");
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
                    steps.add(new TestStep(cell(row, columns, "subfeature", formatter), testCaseId, stepNumber,
                            cell(row, columns, "description", formatter), action,
                            cell(row, columns, "target", formatter),
                            cell(row, columns, "input", formatter),
                            cell(row, columns, "expected", formatter), timeout, enabled));
                } catch (RuntimeException error) {
                    throw new IllegalArgumentException("Dòng Excel " + (index + 1) + ": " + error.getMessage(), error);
                }
            }
        } catch (IOException error) {
            throw new IllegalStateException("Không đọc được file Excel", error);
        }
        steps.sort(Comparator.comparing(TestStep::testCaseId).thenComparingInt(TestStep::stepNumber));
        if (steps.isEmpty()) throw new IllegalArgumentException("Không có bước kiểm thử hợp lệ");
        if (steps.stream().noneMatch(TestStep::enabled)) warnings.add("Tất cả các bước đang bị tắt");
        return new ImportResult(List.copyOf(steps), List.copyOf(warnings));
    }

    private static Sheet findSheet(Workbook workbook, String requestedSheet) {
        String wanted = normalize(requestedSheet);
        for (Sheet sheet : workbook) {
            if (normalize(sheet.getSheetName()).equals(wanted)) return sheet;
        }
        return null;
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
            String key = canonicalHeader(formatter.formatCellValue(cell));
            if (!key.isBlank()) result.put(key, cell.getColumnIndex());
        }
        return result;
    }

    private static String normalize(String value) {
        if (value == null) return "";
        String normalized = java.text.Normalizer.normalize(value, java.text.Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "");
        return normalized.toLowerCase(Locale.ROOT).replaceAll("[^a-z0-9]", "");
    }

    private static String canonicalHeader(String value) {
        String key = normalize(value);
        switch (key) {
            case "testcaseid": case "matestcase": case "matestcaseid": case "mactestcase": return "testcaseid";
            case "step": case "buoc": case "thu tu": case "thutu": return "step";
            case "action": case "thaotac": case "hanhdong": return "action";
            case "description": case "mota": return "description";
            case "subfeature": case "chucnangcon": case "chucnangchitiet": case "mucchucnang": case "nhomcon": return "subfeature";
            case "target": case "doituong": case "bieu tuong": return "target";
            case "input": case "dulieuvao": case "giatrivao": return "input";
            case "expected": case "ketquamongdoi": return "expected";
            case "timeoutms": case "thoigiancho": case "thoigianchoms": return "timeoutms";
            case "enabled": case "kichhoat": case "bat": return "enabled";
            default: return key;
        }
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
