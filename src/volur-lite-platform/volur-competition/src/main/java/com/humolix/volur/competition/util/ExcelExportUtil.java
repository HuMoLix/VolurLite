package com.humolix.volur.competition.util;

import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.streaming.SXSSFCell;
import org.apache.poi.xssf.streaming.SXSSFRow;
import org.apache.poi.xssf.streaming.SXSSFSheet;
import org.apache.poi.xssf.streaming.SXSSFWorkbook;

import java.io.IOException;
import java.io.OutputStream;
import java.util.List;

public final class ExcelExportUtil {

    private ExcelExportUtil() {
    }

    public record SheetData(String sheetName, List<String> headers, List<List<String>> rows) {}

    public static void writeSingleSheet(OutputStream os, String sheetName, List<String> headers, List<List<String>> rows) throws IOException {
        writeSheets(os, List.of(new SheetData(sheetName, headers, rows)));
    }

    public static void writeMultiSheet(OutputStream os,
                                      String sheetName1, List<String> headers1, List<List<String>> rows1,
                                      String sheetName2, List<String> headers2, List<List<String>> rows2) throws IOException {
        if (sheetName2 != null && headers2 != null) {
            writeSheets(os, List.of(
                    new SheetData(sheetName1, headers1, rows1),
                    new SheetData(sheetName2, headers2, rows2)
            ));
        } else {
            writeSingleSheet(os, sheetName1, headers1, rows1);
        }
    }

    public static void writeSheets(OutputStream os, List<SheetData> sheets) throws IOException {
        try (SXSSFWorkbook workbook = new SXSSFWorkbook(100)) {
            workbook.setCompressTempFiles(true);
            if (sheets != null) {
                for (SheetData s : sheets) {
                    if (s != null && s.sheetName() != null && s.headers() != null) {
                        createSheetWithData(workbook, s.sheetName(), s.headers(), s.rows());
                    }
                }
            }
            workbook.write(os);
            workbook.dispose();
        }
    }

    private static void createSheetWithData(SXSSFWorkbook workbook, String sheetName, List<String> headers, List<List<String>> rows) {
        SXSSFSheet sheet = workbook.createSheet(sheetName);
        sheet.trackAllColumnsForAutoSizing();

        CellStyle headerStyle = workbook.createCellStyle();
        Font headerFont = workbook.createFont();
        headerFont.setBold(true);
        headerFont.setFontHeightInPoints((short) 11);
        headerStyle.setFont(headerFont);
        headerStyle.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        headerStyle.setAlignment(HorizontalAlignment.CENTER);
        headerStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        headerStyle.setBorderTop(BorderStyle.THIN);
        headerStyle.setBorderBottom(BorderStyle.THIN);
        headerStyle.setBorderLeft(BorderStyle.THIN);
        headerStyle.setBorderRight(BorderStyle.THIN);

        CellStyle dataStyle = workbook.createCellStyle();
        Font dataFont = workbook.createFont();
        dataFont.setFontHeightInPoints((short) 10);
        dataStyle.setFont(dataFont);
        dataStyle.setVerticalAlignment(VerticalAlignment.CENTER);
        dataStyle.setBorderTop(BorderStyle.THIN);
        dataStyle.setBorderBottom(BorderStyle.THIN);
        dataStyle.setBorderLeft(BorderStyle.THIN);
        dataStyle.setBorderRight(BorderStyle.THIN);

        int rowIdx = 0;
        if (headers != null && !headers.isEmpty()) {
            SXSSFRow headerRow = sheet.createRow(rowIdx++);
            headerRow.setHeightInPoints(24);
            for (int i = 0; i < headers.size(); i++) {
                SXSSFCell cell = headerRow.createCell(i);
                cell.setCellValue(headers.get(i));
                cell.setCellStyle(headerStyle);
            }
        }

        if (rows != null) {
            for (List<String> rowData : rows) {
                SXSSFRow row = sheet.createRow(rowIdx++);
                row.setHeightInPoints(20);
                for (int i = 0; i < rowData.size(); i++) {
                    SXSSFCell cell = row.createCell(i);
                    cell.setCellValue(rowData.get(i) != null ? rowData.get(i) : "");
                    cell.setCellStyle(dataStyle);
                }
            }
        }

        if (headers != null) {
            for (int i = 0; i < headers.size(); i++) {
                sheet.autoSizeColumn(i);
                int currentWidth = sheet.getColumnWidth(i);
                sheet.setColumnWidth(i, Math.max(currentWidth + 1024, 3000));
            }
        }
    }

}
