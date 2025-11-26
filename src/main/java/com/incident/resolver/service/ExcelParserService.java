package com.incident.resolver.service;

import com.incident.resolver.entity.DailyIncident;
import com.incident.resolver.entity.Person;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ExcelParserService {

    public List<DailyIncident> parseExcel(String filePath) throws IOException {
        List<DailyIncident> incidents = new ArrayList<>();
        Map<String, Person> personMap = new HashMap<>();  // Cache persons to avoid duplicates

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);  // Assume first sheet (Sheet1)
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {  // Skip header row (index 0)
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Extract values: Col 0=dayId, Col 1=Person Name, Col 2=Count
                Integer dayId = getCellValueAsInteger(row.getCell(0));
                String personName = getCellValueAsString(row.getCell(1));
                Integer count = getCellValueAsInteger(row.getCell(2));

                // Skip invalid rows
                if (dayId == null || personName == null || personName.trim().isEmpty() || count == null || count < 0) {
                    continue;
                }

                LocalDate date = mapDayIdToDate(dayId);
                if (date == null) continue;  // Invalid date mapping

                // Create or get cached person
                Person person = personMap.computeIfAbsent(personName, k -> new Person(personName));

                // Create incident
                DailyIncident incident = new DailyIncident(dayId, date, count, person);
                incidents.add(incident);
            }
        } catch (IOException e) {
            throw new IOException("Failed to parse Excel file: " + filePath + ". Error: " + e.getMessage(), e);
        }

        return incidents;
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        CellType cellType = cell.getCellType();
        if (cellType == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();  // Assume integer values
        } else if (cellType == CellType.STRING) {
            try {
                return Integer.parseInt(cell.getStringCellValue().trim());
            } catch (NumberFormatException e) {
                return null;
            }
        }
        return null;
    }

    private String getCellValueAsString(Cell cell) {
        if (cell == null) return null;
        CellType cellType = cell.getCellType();
        if (cellType == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cellType == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return null;
    }

    private LocalDate mapDayIdToDate(Integer dayId) {
        // Full hardcoded mapping for your data (Sept 2025: 45901 = 9/1/2025 to 45928 = 9/28/2025)
        return switch (dayId) {
            case 45901 -> LocalDate.of(2025, 9, 1);
            case 45902 -> LocalDate.of(2025, 9, 2);
            case 45903 -> LocalDate.of(2025, 9, 3);
            case 45904 -> LocalDate.of(2025, 9, 4);
            case 45905 -> LocalDate.of(2025, 9, 5);
            case 45906 -> LocalDate.of(2025, 9, 6);
            case 45907 -> LocalDate.of(2025, 9, 7);
            case 45908 -> LocalDate.of(2025, 9, 8);
            case 45909 -> LocalDate.of(2025, 9, 9);
            case 45910 -> LocalDate.of(2025, 9, 10);
            case 45911 -> LocalDate.of(2025, 9, 11);
            case 45912 -> LocalDate.of(2025, 9, 12);
            case 45913 -> LocalDate.of(2025, 9, 13);
            case 45914 -> LocalDate.of(2025, 9, 14);
            case 45915 -> LocalDate.of(2025, 9, 15);
            case 45916 -> LocalDate.of(2025, 9, 16);
            case 45917 -> LocalDate.of(2025, 9, 17);
            case 45918 -> LocalDate.of(2025, 9, 18);
            case 45919 -> LocalDate.of(2025, 9, 19);
            case 45920 -> LocalDate.of(2025, 9, 20);
            case 45921 -> LocalDate.of(2025, 9, 21);
            case 45922 -> LocalDate.of(2025, 9, 22);
            case 45923 -> LocalDate.of(2025, 9, 23);
            case 45924 -> LocalDate.of(2025, 9, 24);
            case 45925 -> LocalDate.of(2025, 9, 25);
            case 45926 -> LocalDate.of(2025, 9, 26);
            case 45927 -> LocalDate.of(2025, 9, 27);
            case 45928 -> LocalDate.of(2025, 9, 28);
            default -> null;  // Invalid dayId - skip row
        };
    }
}