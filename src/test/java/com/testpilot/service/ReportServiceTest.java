package com.testpilot.service;

import com.testpilot.model.entity.TestRun;
import com.testpilot.model.entity.TestStep;
import com.testpilot.model.enums.ActionType;
import com.testpilot.model.enums.RunStatus;
import com.testpilot.model.response.StepResult;
import com.testpilot.service.impl.ReportServiceImpl;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

class ReportServiceTest {
    @TempDir
    Path tempDir;

    @Test
    void exportsDetailSheetLikeInputTemplateWithOnlyResultAdded() throws Exception {
        TestRun run = new TestRun("run-1", 1, 2, "Dự án GPG", "Bán hàng",
                "testcase.xlsx", RunStatus.PASSED, 100, 1, 0, "", null,
                LocalDateTime.of(2026, 8, 4, 10, 0), LocalDateTime.of(2026, 8, 4, 10, 1), tempDir);
        TestStep step = new TestStep("Danh sách", "TC_SALES_LIST_01", 1, "Mở danh sách bán hàng",
                ActionType.CLICK, "testid=sales-menu", "ABC", "Hiển thị danh sách", 5000, true);

        Path report = new ReportServiceImpl().writeExcelReport(run,
                List.of(new StepResult(step, true, "OK", "", Duration.ofMillis(35), null)),
                tempDir.resolve("test-results.xlsx"));

        try (InputStream input = Files.newInputStream(report); Workbook workbook = WorkbookFactory.create(input)) {
            Sheet sheet = workbook.getSheet("Bán hàng");
            Row header = sheet.getRow(0);
            DataFormatter formatter = new DataFormatter();
            String[] expectedHeaders = {"Mã testcase", "Bước", "Mô tả", "Thao tác", "Đối tượng", "Dữ liệu vào",
                    "Kết quả mong đợi", "Thời gian chờ (ms)", "Kích hoạt", "Chức năng con", "Kết quả"};
            for (int index = 0; index < expectedHeaders.length; index++) {
                assertEquals(expectedHeaders[index], formatter.formatCellValue(header.getCell(index)));
            }
            assertNull(header.getCell(expectedHeaders.length));
            Row data = sheet.getRow(1);
            assertEquals("TC_SALES_LIST_01", formatter.formatCellValue(data.getCell(0)));
            assertEquals("TRUE", formatter.formatCellValue(data.getCell(8)));
            assertEquals("Danh sách", formatter.formatCellValue(data.getCell(9)));
            assertEquals("PASS", formatter.formatCellValue(data.getCell(10)));
        }
    }
}
