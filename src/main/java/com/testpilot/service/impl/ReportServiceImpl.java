package com.testpilot.service.impl;

import com.testpilot.model.entity.TestRun;
import com.testpilot.model.response.StepResult;
import com.testpilot.service.ReportService;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.List;

public final class ReportServiceImpl implements ReportService {
    private static final DateTimeFormatter TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Override
    public Path writeExcelReport(TestRun run, List<StepResult> results, Path outputFile) {
        try {
            Files.createDirectories(outputFile.getParent());
            try (Workbook workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(outputFile)) {
                Sheet summary = workbook.createSheet("Tổng quan");
                Sheet detail = workbook.createSheet(safeSheetName(run.featureName()));
                createSummary(workbook, summary, run, results);
                createDetail(workbook, detail, results);
                workbook.write(output);
            }
            return outputFile;
        } catch (IOException error) {
            throw new IllegalStateException("Không xuất được báo cáo Excel", error);
        }
    }

    @Override
    public Path writeLog(TestRun run, List<StepResult> results, Path outputFile) {
        StringBuilder log = new StringBuilder();
        log.append("TestPilot Studio - Lần chạy ").append(run.id()).append('\n');
        log.append("Dự án: ").append(run.projectName()).append(" / ").append(run.featureName()).append('\n');
        log.append("Bắt đầu: ").append(run.startedAt().format(TIME)).append("\n\n");
        for (StepResult result : results) {
            log.append(result.passed() ? "ĐẠT" : "KHÔNG ĐẠT").append(" | ")
                    .append(result.step().testCaseId()).append(" #").append(result.step().stepNumber())
                    .append(" | ").append(result.step().action()).append(" | ")
                    .append(result.duration().toMillis()).append(" ms");
            if (!result.passed()) log.append(" | ").append(result.error());
            log.append('\n');
        }
        try {
            Files.writeString(outputFile, log.toString(), StandardCharsets.UTF_8);
            return outputFile;
        } catch (IOException error) {
            throw new IllegalStateException("Không ghi được nhật ký", error);
        }
    }

    private static void createSummary(Workbook workbook, Sheet sheet, TestRun run, List<StepResult> results) {
        CellStyle label = headerStyle(workbook, IndexedColors.DARK_BLUE);
        CellStyle value = workbook.createCellStyle();
        String[][] values = {
                {"Mã lần chạy", run.id()},
                {"Dự án", run.projectName()},
                {"Chức năng", run.featureName()},
                {"Tệp đầu vào", run.sourceFile()},
                {"Bắt đầu", run.startedAt().format(TIME)},
                {"Tổng số bước", Integer.toString(results.size())},
                {"Đạt", Long.toString(results.stream().filter(StepResult::passed).count())},
                {"Không đạt", Long.toString(results.stream().filter(result -> !result.passed()).count())}
        };
        for (int i = 0; i < values.length; i++) {
            Row row = sheet.createRow(i);
            Cell first = row.createCell(0);
            first.setCellValue(values[i][0]);
            first.setCellStyle(label);
            Cell second = row.createCell(1);
            second.setCellValue(values[i][1]);
            second.setCellStyle(value);
        }
        sheet.setColumnWidth(0, 22 * 256);
        sheet.setColumnWidth(1, 70 * 256);
    }

    private static void createDetail(Workbook workbook, Sheet sheet, List<StepResult> results) {
        String[] headers = {"Mã testcase", "Bước", "Mô tả", "Thao tác", "Đối tượng", "Dữ liệu vào", "Kết quả mong đợi",
                "Thời gian chờ (ms)", "Kích hoạt", "Chức năng con", "Kết quả"};
        Row header = sheet.createRow(0);
        CellStyle headerStyle = headerStyle(workbook, IndexedColors.DARK_BLUE);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = header.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(headerStyle);
        }
        CellStyle passStyle = statusStyle(workbook, IndexedColors.LIGHT_GREEN);
        CellStyle failStyle = statusStyle(workbook, IndexedColors.ROSE);
        for (int i = 0; i < results.size(); i++) {
            StepResult result = results.get(i);
            Row row = sheet.createRow(i + 1);
            String[] values = {
                    result.step().testCaseId(), Integer.toString(result.step().stepNumber()), result.step().description(),
                    result.step().action().name(), result.step().target(), result.step().input(), result.step().expected(),
                    Integer.toString(result.step().timeoutMs()), Boolean.toString(result.step().enabled()).toUpperCase(),
                    result.step().subFeatureName(), result.passed() ? "PASS" : "FAIL"
            };
            for (int col = 0; col < values.length; col++) {
                Cell cell = row.createCell(col);
                cell.setCellValue(values[col] == null ? "" : values[col]);
                if (col == 10) cell.setCellStyle(result.passed() ? passStyle : failStyle);
            }
        }
        sheet.createFreezePane(0, 1);
        sheet.setAutoFilter(new org.apache.poi.ss.util.CellRangeAddress(0, Math.max(1, results.size()), 0, headers.length - 1));
        int[] widths = {18, 8, 36, 20, 34, 28, 32, 18, 12, 24, 14};
        for (int i = 0; i < widths.length; i++) sheet.setColumnWidth(i, widths[i] * 256);
    }

    private static String safeSheetName(String value) {
        String name = value == null ? "Kết quả kiểm thử" : value.replaceAll("[\\\\/:*?\"<>|]", "-").trim();
        if (name.isBlank()) name = "Kết quả kiểm thử";
        return name.length() <= 31 ? name : name.substring(0, 31);
    }

    private static CellStyle headerStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        style.setFont(font);
        return style;
    }

    private static CellStyle statusStyle(Workbook workbook, IndexedColors color) {
        CellStyle style = workbook.createCellStyle();
        style.setFillForegroundColor(color.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        Font font = workbook.createFont();
        font.setBold(true);
        style.setFont(font);
        return style;
    }
}
