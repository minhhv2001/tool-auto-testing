package com.testpilot.service;

import com.testpilot.model.enums.ActionType;
import com.testpilot.model.response.ImportResult;
import com.testpilot.service.impl.ExcelServiceImpl;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

class ExcelServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void importsTheDeliveredAutomationTemplate() {
        ImportResult result = new ExcelServiceImpl().importAutomationSteps(
                Path.of("sample-data/AUTO_TESTING_IMD_Automation_Template.xlsx"));
        assertEquals(2, result.testCaseCount());
        assertEquals(8, result.steps().size());
        assertEquals(6, result.steps().stream().filter(step -> step.enabled()).count());
        assertEquals(1, result.steps().stream()
                .filter(step -> step.action() == ActionType.EXPECT_ROWS_CONTAIN).count());
    }

    @Test
    void importsVietnameseSubFeaturesFromOneFunctionSheet() {
        ImportResult result = new ExcelServiceImpl().importAutomationSteps(
                Path.of("sample-data/AUTO_TESTING_IMD_BanHang_CayChucNang.xlsx"), "Bán hàng");
        assertEquals(4, result.testCaseCount());
        assertEquals(11, result.steps().size());
        assertEquals("Danh sách", result.steps().stream()
                .filter(step -> step.testCaseId().equals("TC_SALES_LIST_01"))
                .findFirst().orElseThrow().subFeatureName());
        assertEquals("Thêm mới", result.steps().stream()
                .filter(step -> step.testCaseId().equals("TC_SALES_CREATE_01"))
                .findFirst().orElseThrow().subFeatureName());
    }

    @Test
    void importsCompanyManualTemplateFromConfiguredStartCell() throws Exception {
        Path file = tempDir.resolve("manual-template.xlsx");
        try (Workbook workbook = new XSSFWorkbook(); OutputStream output = Files.newOutputStream(file)) {
            Sheet sheet = workbook.createSheet("Bán hàng");
            Row header = sheet.createRow(1);
            header.createCell(1).setCellValue("Test Case ID");
            header.createCell(2).setCellValue("Test Case Description");
            header.createCell(3).setCellValue("Test Case Purpose");
            header.createCell(4).setCellValue("Steps");
            header.createCell(5).setCellValue("Expected Output");
            header.createCell(6).setCellValue("Bug description");
            sheet.createRow(2).createCell(1).setCellValue("1_TC_QLNN_02: Tab Người thân");
            sheet.createRow(3).createCell(2).setCellValue("Danh sách Người thân");
            Row firstCase = sheet.createRow(4);
            firstCase.createCell(2).setCellValue("Kiểm tra hiển thị thông tin Nhân viên - Tab Người thân");
            firstCase.createCell(3).setCellValue("Thông tin");
            firstCase.createCell(4).setCellValue("1. Chọn chức năng Nhân viên - Tab Người thân\n2. Chọn Tab Người thân");
            firstCase.createCell(5).setCellValue("1. Hiển thị đầy đủ thông tin theo giao diện yêu cầu");
            Row secondCase = sheet.createRow(5);
            secondCase.createCell(2).setCellValue("Kiểm tra danh sách người thân của nhân viên");
            secondCase.createCell(3).setCellValue("Danh sách người thân");
            secondCase.createCell(4).setCellValue("1. Tải màn Nhân viên - Tab Người thân");
            secondCase.createCell(5).setCellValue("1. Danh sách gồm tất cả người thân của nhân viên");
            workbook.write(output);
        }

        ImportResult result = new ExcelServiceImpl().importAutomationSteps(file, "Bán hàng", 2, 2);

        assertEquals(2, result.testCaseCount());
        assertEquals(2, result.steps().size());
        assertEquals("Danh sách Người thân", result.steps().get(0).subFeatureName());
        assertEquals("Kiểm tra hiển thị thông tin Nhân viên - Tab Người thân · Thông tin",
                result.steps().get(0).description());
        assertFalse(result.steps().get(0).enabled());
        assertFalse(result.warnings().isEmpty());
    }
}
