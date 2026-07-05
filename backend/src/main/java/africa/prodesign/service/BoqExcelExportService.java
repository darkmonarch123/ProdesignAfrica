package africa.prodesign.service;

import africa.prodesign.dto.BoqLineItem;
import africa.prodesign.dto.BoqResponse;
import africa.prodesign.entity.Project;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.time.format.DateTimeFormatter;
import java.time.ZoneOffset;

@Service
public class BoqExcelExportService {

    public byte[] export(Project project, BoqResponse boq) {
        try (Workbook workbook = new XSSFWorkbook(); ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("Bill of Quantities");
            sheet.setColumnWidth(0, 5 * 256);
            sheet.setColumnWidth(1, 45 * 256);
            sheet.setColumnWidth(2, 12 * 256);
            sheet.setColumnWidth(3, 10 * 256);
            sheet.setColumnWidth(4, 16 * 256);
            sheet.setColumnWidth(5, 18 * 256);

            CellStyle titleStyle = titleStyle(workbook);
            CellStyle subtitleStyle = subtitleStyle(workbook);
            CellStyle headerStyle = headerStyle(workbook);
            CellStyle categoryStyle = categoryStyle(workbook);
            CellStyle textStyle = textStyle(workbook);
            CellStyle numberStyle = numberStyle(workbook);
            CellStyle currencyStyle = currencyStyle(workbook);
            CellStyle totalLabelStyle = totalLabelStyle(workbook);
            CellStyle totalValueStyle = totalValueStyle(workbook);
            CellStyle noteStyle = noteStyle(workbook);

            int rowIdx = 0;

            Row title = sheet.createRow(rowIdx++);
            setCell(title, 0, "Bill of Quantities — " + project.getName(), titleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(0, 0, 0, 5));

            Row subtitle = sheet.createRow(rowIdx++);
            String generatedDate = boq.generatedAt().atZone(ZoneOffset.UTC).format(DateTimeFormatter.ofPattern("dd MMM yyyy"));
            setCell(subtitle, 0, "Currency: " + boq.currency() + "   ·   Generated: " + generatedDate, subtitleStyle);
            sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(1, 1, 0, 5));

            rowIdx++; // spacer row

            Row header = sheet.createRow(rowIdx++);
            String[] headers = {"S/N", "Description", "Quantity", "Unit", "Rate", "Amount"};
            for (int i = 0; i < headers.length; i++) {
                setCell(header, i, headers[i], headerStyle);
            }

            int serial = 1;
            String currentCategory = null;
            for (BoqLineItem item : boq.lineItems()) {
                if (!item.category().equals(currentCategory)) {
                    currentCategory = item.category();
                    Row categoryRow = sheet.createRow(rowIdx++);
                    setCell(categoryRow, 0, currentCategory, categoryStyle);
                    sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));
                }
                Row row = sheet.createRow(rowIdx++);
                setCell(row, 0, String.valueOf(serial++), numberStyle);
                setCell(row, 1, item.description(), textStyle);
                setCell(row, 2, item.quantity(), numberStyle);
                setCell(row, 3, item.unit(), textStyle);
                setCell(row, 4, item.rate(), currencyStyle);
                setCell(row, 5, item.amount(), currencyStyle);
            }

            rowIdx++; // spacer row
            Row totalRow = sheet.createRow(rowIdx++);
            setCell(totalRow, 0, "", totalLabelStyle);
            setCell(totalRow, 1, "", totalLabelStyle);
            setCell(totalRow, 2, "", totalLabelStyle);
            setCell(totalRow, 3, "", totalLabelStyle);
            setCell(totalRow, 4, "TOTAL", totalLabelStyle);
            setCell(totalRow, 5, boq.totalAmount(), totalValueStyle);

            rowIdx++; // spacer row
            for (String note : boq.notes()) {
                Row noteRow = sheet.createRow(rowIdx++);
                setCell(noteRow, 0, "Note: " + note, noteStyle);
                sheet.addMergedRegion(new org.apache.poi.ss.util.CellRangeAddress(rowIdx - 1, rowIdx - 1, 0, 5));
            }

            workbook.write(out);
            return out.toByteArray();
        } catch (IOException e) {
            throw new UncheckedIOException("Failed to generate BOQ workbook", e);
        }
    }

    private void setCell(Row row, int col, String value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.STRING);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private void setCell(Row row, int col, double value, CellStyle style) {
        Cell cell = row.createCell(col, CellType.NUMERIC);
        cell.setCellValue(value);
        cell.setCellStyle(style);
    }

    private CellStyle titleStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setFontHeightInPoints((short) 14);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle subtitleStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 10);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        return style;
    }

    private CellStyle headerStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        font.setColor(IndexedColors.WHITE.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(new org.apache.poi.xssf.usermodel.XSSFColor(new byte[]{4, 120, (byte) 87}, null));
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        style.setAlignment(HorizontalAlignment.CENTER);
        addBorder(style);
        return style;
    }

    private CellStyle categoryStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
        style.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        return style;
    }

    private CellStyle textStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        addBorder(style);
        style.setWrapText(true);
        return style;
    }

    private CellStyle numberStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        addBorder(style);
        style.setAlignment(HorizontalAlignment.CENTER);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("0.00"));
        return style;
    }

    private CellStyle currencyStyle(Workbook wb) {
        CellStyle style = wb.createCellStyle();
        addBorder(style);
        style.setAlignment(HorizontalAlignment.RIGHT);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private CellStyle totalLabelStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setAlignment(HorizontalAlignment.RIGHT);
        style.setBorderTop(BorderStyle.MEDIUM);
        return style;
    }

    private CellStyle totalValueStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setBold(true);
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setBorderTop(BorderStyle.MEDIUM);
        DataFormat format = wb.createDataFormat();
        style.setDataFormat(format.getFormat("#,##0.00"));
        return style;
    }

    private CellStyle noteStyle(Workbook wb) {
        Font font = wb.createFont();
        font.setItalic(true);
        font.setFontHeightInPoints((short) 9);
        font.setColor(IndexedColors.GREY_50_PERCENT.getIndex());
        CellStyle style = wb.createCellStyle();
        style.setFont(font);
        style.setWrapText(true);
        return style;
    }

    private void addBorder(CellStyle style) {
        style.setBorderTop(BorderStyle.THIN);
        style.setBorderBottom(BorderStyle.THIN);
        style.setBorderLeft(BorderStyle.THIN);
        style.setBorderRight(BorderStyle.THIN);
    }
}
