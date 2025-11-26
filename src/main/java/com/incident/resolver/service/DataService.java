package com.incident.resolver.service;

import com.incident.resolver.entity.DailyIncident;
import com.incident.resolver.entity.Person;
import com.incident.resolver.repository.DailyIncidentRepository;
import com.incident.resolver.repository.PersonRepository;
import org.apache.poi.ss.usermodel.*;
//import org.apache.poi.ss.util.IndexedColors;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.openxml4j.util.ZipSecureFile;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.*;

@Service
public class DataService {

    @Autowired
    private PersonRepository personRepo;

    @Autowired
    private DailyIncidentRepository incidentRepo;

    @Autowired
    private ExcelParserService parser;

    public void loadFromExcel(String filePath) throws Exception {
        List<DailyIncident> incidents = parser.parseExcel(filePath);
        for (DailyIncident inc : incidents) {
            Person person = personRepo.findByName(inc.getPerson().getName());
            if (person == null) {
                person = inc.getPerson();
                personRepo.save(person);
            }
            inc.setPerson(person);
            incidentRepo.save(inc);
        }
        System.out.println("Data loaded to MySQL DB! Total incidents: " + incidents.size());
    }

    public void generateReport(String outputFilePath) {
        List<Object[]> allDays = incidentRepo.findAllDailyResolutions();
        Map<String, Map<LocalDate, Integer>> personDailyMap = new TreeMap<>();
        Set<LocalDate> allDates = new HashSet<>();
        int grandTotalResolved = 0;

        for (Object[] row : allDays) {
            String name = (String) row[0];
            LocalDate date = (LocalDate) row[1];
            Integer count = (Integer) row[2];
            allDates.add(date);
            personDailyMap.computeIfAbsent(name, k -> new TreeMap<>()).put(date, count);
            grandTotalResolved += count;
        }

        LocalDate minDate = allDates.stream().min(LocalDate::compareTo).orElse(LocalDate.now());
        LocalDate maxDate = allDates.stream().max(LocalDate::compareTo).orElse(LocalDate.now());
        long totalDays = ChronoUnit.DAYS.between(minDate, maxDate) + 1;  // Dynamic

        System.out.println("=== INCIDENT RESOLUTION REPORT (" + minDate.getYear() + ") ===");
        System.out.println("Date Range: " + minDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " to " + maxDate.format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + " | Total Days: " + totalDays + " | Grand Total Resolved: " + grandTotalResolved + "\n");

        List<String> personNames = new ArrayList<>(personDailyMap.keySet());
        Collections.sort(personNames);

        Map<LocalDate, Map<String, Integer>> pivotedData = new TreeMap<>();
        Map<String, Object[]> summaries = new HashMap<>();

        // Fill pivoted with zeros for missing
        for (LocalDate date = minDate; !date.isAfter(maxDate); date = date.plusDays(1)) {
            Map<String, Integer> dateCounts = new HashMap<>();
            for (String name : personNames) {
                Map<LocalDate, Integer> personDays = personDailyMap.getOrDefault(name, new TreeMap<>());
                dateCounts.put(name, personDays.getOrDefault(date, 0));
            }
            pivotedData.put(date, dateCounts);
        }

        for (String name : personNames) {
            Map<LocalDate, Integer> dailyCounts = personDailyMap.getOrDefault(name, new TreeMap<>());
            int totalIncidents = dailyCounts.values().stream().mapToInt(Integer::intValue).sum();
            long workingDays = dailyCounts.size();
            int zeroDays = (int) totalDays - (int) workingDays;
            double avg = workingDays > 0 ? (double) totalIncidents / workingDays : 0;
            double percentTotal = grandTotalResolved > 0 ? (double) totalIncidents / grandTotalResolved * 100 : 0;

            System.out.println("Person: " + name);
            System.out.println("Daily Resolutions:");
            for (Map.Entry<LocalDate, Integer> dayEntry : dailyCounts.entrySet()) {
                String dateStr = dayEntry.getKey().format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
                System.out.println("  " + dateStr + ": " + dayEntry.getValue() + " incidents");
            }
            System.out.println("Total Incidents: " + totalIncidents);
            System.out.println("Zero Days: " + zeroDays + " (out of " + totalDays + ")");
            System.out.println("Average per Working Day: " + String.format("%.2f", avg));
            System.out.println("Contribution %: " + String.format("%.2f", percentTotal) + "%\n");

            Object[] summary = {totalIncidents, zeroDays, avg, percentTotal};
            summaries.put(name, summary);
        }

        exportPivotedToExcel(pivotedData, personNames, summaries, grandTotalResolved, outputFilePath, (int) totalDays);
    }

    private void exportPivotedToExcel(Map<LocalDate, Map<String, Integer>> pivotedData, List<String> personNames,
                                      Map<String, Object[]> summaries, int grandTotal, String filePath, int totalDays) {
        Workbook workbook = null;
        boolean newFile = false;

        // FIX: Lower ZIP bomb threshold for XLSX files
        ZipSecureFile.setMinInflateRatio(0.002);  // Allow lower ratio to prevent "Zip bomb" error

        try {
            java.io.File file = new java.io.File(filePath);
            if (!file.exists()) {
                workbook = new XSSFWorkbook();  // New workbook
                newFile = true;
                System.out.println("Created new Excel file: " + filePath);
            } else {
                try (FileInputStream fis = new FileInputStream(filePath)) {
                    workbook = new XSSFWorkbook(fis);
                }
            }

            // Remove existing "Report" sheet to avoid duplicate error
            Sheet existingSheet = workbook.getSheet("Report");
            if (existingSheet != null) {
                int sheetIndex = workbook.getSheetIndex(existingSheet);
                workbook.removeSheetAt(sheetIndex);
                System.out.println("Removed existing 'Report' sheet to avoid duplicate.");
            }

            Sheet reportSheet = workbook.createSheet("Report");

            // Headers
            Row headerRow = reportSheet.createRow(0);
            headerRow.createCell(0).setCellValue("Date");
            CellStyle headerStyle = workbook.createCellStyle();
            Font boldFont = workbook.createFont();
            boldFont.setBold(true);
            headerStyle.setFont(boldFont);
            headerStyle.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());
            headerRow.getCell(0).setCellStyle(headerStyle);

            int col = 1;
            for (String name : personNames) {
                Cell cell = headerRow.createCell(col++);
                cell.setCellValue(name);
                cell.setCellStyle(headerStyle);
            }

            // Data Rows
            int rowNum = 1;
            for (Map.Entry<LocalDate, Map<String, Integer>> dateEntry : pivotedData.entrySet()) {
                LocalDate date = dateEntry.getKey();
                Row dataRow = reportSheet.createRow(rowNum++);
                dataRow.createCell(0).setCellValue(date.format(DateTimeFormatter.ofPattern("M/d/yyyy")));

                int dataCol = 1;
                for (String name : personNames) {
                    Integer count = dateEntry.getValue().get(name);
                    Cell countCell = dataRow.createCell(dataCol++);
                    countCell.setCellValue(count);
                    CellStyle style = workbook.createCellStyle();
                    if (count > 0) {
                        style.setFillForegroundColor(IndexedColors.LIGHT_GREEN.getIndex());
                    } else {
                        style.setFillForegroundColor(IndexedColors.LIGHT_BLUE.getIndex());  // FIX: Red for 0
                    }
                    countCell.setCellStyle(style);
                }
            }

            // Summary Rows
            String[] summaryLabels = {"Total Incidents", "Zero Days (out of " + totalDays + ")", "Average per Working Day", "% of Grand Total"};
            for (int s = 0; s < summaryLabels.length; s++) {
                Row sumRow = reportSheet.createRow(rowNum++);
                sumRow.createCell(0).setCellValue(summaryLabels[s]);

                int sumCol = 1;
                for (String name : personNames) {
                    Cell sumCell = sumRow.createCell(sumCol++);
                    Object[] vals = summaries.getOrDefault(name, new Object[]{0, 0, 0.0, 0.0});
                    switch (s) {
                        case 0 -> sumCell.setCellValue((Integer) vals[0]);
                        case 1 -> sumCell.setCellValue((Integer) vals[1]);
                        case 2 -> sumCell.setCellValue(String.format("%.2f", (Double) vals[2]));
                        case 3 -> sumCell.setCellValue(String.format("%.2f", (Double) vals[3]));
                    }
                    CellStyle sumStyle = workbook.createCellStyle();
                    sumStyle.setFont(boldFont);
                    sumStyle.setFillForegroundColor(IndexedColors.YELLOW.getIndex());
                    sumCell.setCellStyle(sumStyle);
                }
            }

            // Auto-size columns
            for (int i = 0; i <= personNames.size(); i++) {
                reportSheet.autoSizeColumn(i);
            }

            // Write to file
            try (FileOutputStream fos = new FileOutputStream(filePath)) {
                workbook.write(fos);
            }

            if (workbook != null) {
                workbook.close();
            }
            System.out.println("Pivoted Report exported to " + filePath + " ('Report' sheet updated)!");

        } catch (IOException e) {
            System.err.println("Export error: " + e.getMessage());
            if (workbook != null) {
                try {
                    workbook.close();
                } catch (IOException closeEx) {
                    System.err.println("Failed to close workbook: " + closeEx.getMessage());
                }
            }
            throw new RuntimeException("Failed to export report: " + e.getMessage(), e);
        }
    }
}