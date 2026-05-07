package com.smartfixsamana.services;

import com.smartfixsamana.models.entities.ExternalRepair;
import com.smartfixsamana.models.enums.ExternalRepairStatus;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Service
public class ExternalRepairExcelService {

    private static final String[] HEADERS = {
            "Cliente", "Marca", "Solución", "Precio Reparación",
            "Costo Repuesto", "Ganancia Neta", "60%+repuesto", "Observación"
    };

    public byte[] exportToExcel(List<ExternalRepair> repairs) throws IOException {
        return exportToExcel(repairs, null);
    }

    public byte[] exportToExcel(List<ExternalRepair> repairs, LocalDate settlementStartDate) throws IOException {
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("Reparaciones Externas");

            // Header style
            CellStyle headerStyle = workbook.createCellStyle();
            Font headerFont = workbook.createFont();
            headerFont.setBold(true);
            headerFont.setColor(IndexedColors.WHITE.getIndex());
            headerStyle.setFont(headerFont);
            headerStyle.setFillForegroundColor(IndexedColors.DARK_TEAL.getIndex());
            headerStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);
            headerStyle.setAlignment(HorizontalAlignment.CENTER);

            // Yellow highlight style for PENDIENTE_RECOGER
            CellStyle yellowStyle = workbook.createCellStyle();
            yellowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            yellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Currency style
            CellStyle currencyStyle = workbook.createCellStyle();
            DataFormat format = workbook.createDataFormat();
            currencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            CellStyle currencyYellowStyle = workbook.createCellStyle();
            currencyYellowStyle.setDataFormat(format.getFormat("#,##0.00"));
            currencyYellowStyle.setFillForegroundColor(IndexedColors.LIGHT_YELLOW.getIndex());
            currencyYellowStyle.setFillPattern(FillPatternType.SOLID_FOREGROUND);

            // Create header row
            Row headerRow = sheet.createRow(0);
            for (int i = 0; i < HEADERS.length; i++) {
                Cell cell = headerRow.createCell(i);
                cell.setCellValue(HEADERS[i]);
                cell.setCellStyle(headerStyle);
            }

            // Data rows
            double totalRepairPrice = 0;
            double totalPartCost = 0;
            double totalNetProfit = 0;
            double totalMyShare = 0;

            for (int i = 0; i < repairs.size(); i++) {
                ExternalRepair repair = repairs.get(i);
                Row row = sheet.createRow(i + 1);
                boolean isPendiente = repair.getStatus() == ExternalRepairStatus.PENDIENTE_RECOGER;
                CellStyle rowCurrencyStyle = isPendiente ? currencyYellowStyle : currencyStyle;
                CellStyle rowTextStyle = isPendiente ? yellowStyle : null;

                Cell c0 = row.createCell(0);
                c0.setCellValue(repair.getClientName());
                if (rowTextStyle != null) c0.setCellStyle(rowTextStyle);

                Cell c1 = row.createCell(1);
                c1.setCellValue(repair.getPhoneBrand());
                if (rowTextStyle != null) c1.setCellStyle(rowTextStyle);

                Cell c2 = row.createCell(2);
                c2.setCellValue(repair.getSolution());
                if (rowTextStyle != null) c2.setCellStyle(rowTextStyle);

                Cell c3 = row.createCell(3);
                c3.setCellValue(repair.getRepairPrice());
                c3.setCellStyle(rowCurrencyStyle);

                double partCost = repair.getPartCost() != null ? repair.getPartCost() : 0.0;
                Cell c4 = row.createCell(4);
                c4.setCellValue(partCost);
                c4.setCellStyle(rowCurrencyStyle);

                Cell c5 = row.createCell(5);
                c5.setCellValue(repair.getNetProfit());
                c5.setCellStyle(rowCurrencyStyle);

                Cell c6 = row.createCell(6);
                c6.setCellValue(repair.getMyShare());
                c6.setCellStyle(rowCurrencyStyle);

                Cell c7 = row.createCell(7);
                boolean isCarriedOver = settlementStartDate != null
                        && repair.getDate().isBefore(settlementStartDate);
                if (isCarriedOver) {
                    c7.setCellValue("Pendiente anterior");
                    if (rowTextStyle != null) c7.setCellStyle(rowTextStyle);
                }

                totalRepairPrice += repair.getRepairPrice();
                totalPartCost += partCost;
                totalNetProfit += repair.getNetProfit();
                totalMyShare += repair.getMyShare();
            }

            // Total row
            CellStyle totalStyle = workbook.createCellStyle();
            Font totalFont = workbook.createFont();
            totalFont.setBold(true);
            totalStyle.setFont(totalFont);

            CellStyle totalCurrencyStyle = workbook.createCellStyle();
            totalCurrencyStyle.setFont(totalFont);
            totalCurrencyStyle.setDataFormat(format.getFormat("#,##0.00"));

            Row totalRow = sheet.createRow(repairs.size() + 1);
            Cell totalLabel = totalRow.createCell(2);
            totalLabel.setCellValue("TOTALES");
            totalLabel.setCellStyle(totalStyle);

            Cell tc3 = totalRow.createCell(3);
            tc3.setCellValue(totalRepairPrice);
            tc3.setCellStyle(totalCurrencyStyle);

            Cell tc4 = totalRow.createCell(4);
            tc4.setCellValue(totalPartCost);
            tc4.setCellStyle(totalCurrencyStyle);

            Cell tc5 = totalRow.createCell(5);
            tc5.setCellValue(totalNetProfit);
            tc5.setCellStyle(totalCurrencyStyle);

            Cell tc6 = totalRow.createCell(6);
            tc6.setCellValue(totalMyShare);
            tc6.setCellStyle(totalCurrencyStyle);

            // Auto-size columns
            for (int i = 0; i < HEADERS.length; i++) {
                sheet.autoSizeColumn(i);
            }

            ByteArrayOutputStream out = new ByteArrayOutputStream();
            workbook.write(out);
            return out.toByteArray();
        }
    }

    /**
     * Parses imported Excel rows into ExcelImportRow records for matching.
     * Columns: Cliente(0), Marca(1), Solución(2), Precio Reparación(3), Costo Repuesto(4), Ganancia Neta(5), 60%+repuesto(6)
     */
    public List<ExcelImportRow> parseImportRows(InputStream inputStream) throws IOException {
        List<ExcelImportRow> results = new ArrayList<>();

        try (Workbook workbook = WorkbookFactory.create(inputStream)) {
            Sheet sheet = workbook.getSheetAt(0);

            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                String clientName = getStringValue(row.getCell(0));
                if (clientName == null || clientName.isBlank()) continue;
                if (clientName.equalsIgnoreCase("TOTALES")) continue;

                String phoneBrand = getStringValue(row.getCell(1));
                String solution = getStringValue(row.getCell(2));
                double repairPrice = getNumericValue(row.getCell(3));

                results.add(new ExcelImportRow(clientName, phoneBrand, solution, repairPrice));
            }
        }

        return results;
    }

    public record ExcelImportRow(String clientName, String phoneBrand, String solution, double repairPrice) {}

    private String getStringValue(Cell cell) {
        if (cell == null) return null;
        return switch (cell.getCellType()) {
            case STRING -> cell.getStringCellValue();
            case NUMERIC -> String.valueOf((long) cell.getNumericCellValue());
            case BOOLEAN -> String.valueOf(cell.getBooleanCellValue());
            default -> null;
        };
    }

    private double getNumericValue(Cell cell) {
        if (cell == null) return 0.0;
        return switch (cell.getCellType()) {
            case NUMERIC -> cell.getNumericCellValue();
            case STRING -> {
                try {
                    yield Double.parseDouble(cell.getStringCellValue().replace(",", ""));
                } catch (NumberFormatException e) {
                    yield 0.0;
                }
            }
            default -> 0.0;
        };
    }
}
