package com.incident.resolver.service;

import com.incident.resolver.entity.DailyIncident;
import com.incident.resolver.entity.Person;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.beans.factory.annotation.Value;
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

    @Value("${app.base.year:2025}")
    private int baseYear;  // Change for different files/years

    @Value("${app.base.month:9}")
    private int baseMonth;  // Change for different months

    @Value("${app.base.day:1}")
    private int baseDay;  // Change for different start day

    @Value("${app.base.dayId:45901}")
    private int baseDayId;  // Base dayId corresponding to base date

    public List<DailyIncident> parseExcel(String filePath) throws IOException {
        List<DailyIncident> incidents = new ArrayList<>();
        Map<String, Person> personMap = new HashMap<>();

        // Dynamic base date - change properties for different files
        LocalDate baseDate = LocalDate.of(baseYear, baseMonth, baseDay);
        System.out.println("Dynamic base date for this file: " + baseDate + " (dayId " + baseDayId + ")");

        try (FileInputStream fis = new FileInputStream(filePath);
             Workbook workbook = new XSSFWorkbook(fis)) {

            Sheet sheet = workbook.getSheetAt(0);
            for (int i = 1; i <= sheet.getLastRowNum(); i++) {
                Row row = sheet.getRow(i);
                if (row == null) continue;

                // Correct columns: Col A (0): dayId, Col B (1): Person Name, Col C (2): Count
                Integer dayId = getCellValueAsInteger(row.getCell(0));  // Col A: dayId (45901 etc.)
                String personName = getCellValueAsString(row.getCell(1));  // Col B: Person (Appalasuri Badithaboni)
                Integer count = getCellValueAsInteger(row.getCell(2));  // Col C: Count (3 etc.)

                // Skip invalid
                if (dayId == null || personName == null || personName.trim().isEmpty() || count == null || count < 0) {
                    continue;
                }

                // Map dayId to date using dynamic base + offset
                LocalDate date = baseDate.plusDays(dayId - baseDayId);
                if (date == null) continue;

                Person person = personMap.computeIfAbsent(personName, k -> new Person(personName));
                DailyIncident incident = new DailyIncident(dayId, date, count, person);
                incidents.add(incident);
            }
        } catch (IOException e) {
            throw new IOException("Failed to parse Excel file: " + filePath + ". Error: " + e.getMessage(), e);
        }

        System.out.println("Parsed " + incidents.size() + " incidents from file.");
        return incidents;
    }

    private Integer getCellValueAsInteger(Cell cell) {
        if (cell == null) return null;
        if (cell.getCellType() == CellType.NUMERIC) {
            return (int) cell.getNumericCellValue();
        } else if (cell.getCellType() == CellType.STRING) {
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
        if (cell.getCellType() == CellType.STRING) {
            return cell.getStringCellValue().trim();
        } else if (cell.getCellType() == CellType.NUMERIC) {
            return String.valueOf((int) cell.getNumericCellValue());
        }
        return null;
    }
}