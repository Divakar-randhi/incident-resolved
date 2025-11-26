package com.incident.resolver.controller;

import com.incident.resolver.service.DataService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api")
@CrossOrigin(origins = "*")
@Tag(name = "Report Controller", description = "Endpoints for uploading Excel and generating reports")
public class ReportController {

    @Autowired
    private DataService dataService;

    @Operation(
        summary = "Upload Excel file for incident data",
        description = "Upload an Excel file (.xlsx) to parse incidents, save to DB, and generate report sheet."
    )
    @ApiResponse(responseCode = "200", description = "Upload successful")
    @ApiResponse(responseCode = "400", description = "Invalid file")
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<Map<String, Object>> uploadExcel(
            @Parameter(description = "Excel file to upload", required = true, content = @Content(mediaType = MediaType.MULTIPART_FORM_DATA_VALUE))
            @RequestPart("file") MultipartFile file) {
        Map<String, Object> response = new HashMap<>();
        try {
            if (file.isEmpty()) {
                response.put("error", "File is empty");
                return ResponseEntity.badRequest().body(response);
            }

            if (!file.getOriginalFilename().toLowerCase().endsWith(".xlsx")) {
                response.put("error", "Only .xlsx files allowed");
                return ResponseEntity.badRequest().body(response);
            }

            // Sanitize file name
            String originalName = file.getOriginalFilename();
            String sanitizedName = originalName.replaceAll("[^a-zA-Z0-9.-]", "_");
            
            // Use system temp dir for reliable write access
            String tempDir = System.getProperty("java.io.tmpdir");
            String filePath = tempDir + "uploaded_" + sanitizedName;
            System.out.println("Saving uploaded file to: " + filePath);  // Debug log

            // Save file
            file.transferTo(new File(filePath));

            // Verify save
            File savedFile = new File(filePath);
            if (!savedFile.exists() || savedFile.length() == 0) {
                response.put("error", "Failed to save uploaded file: " + filePath + " (size: " + (savedFile.exists() ? savedFile.length() : "not found") + "). Check temp dir permissions.");
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
            }
            System.out.println("File saved successfully: " + filePath + " (size: " + savedFile.length() + " bytes)");  // Debug

            // Process: Load to DB + Generate report (updates the file)
            dataService.loadFromExcel(filePath);
            dataService.generateReport(filePath);

            response.put("message", "File uploaded, data loaded to DB, and report generated successfully!");
            response.put("fileName", originalName);
            response.put("processedPath", filePath);
            response.put("recordsProcessed", "Check console/DB for details. Report sheet added to file.");

            // Clean up temp file
            savedFile.delete();
            System.out.println("Temp file cleaned up: " + filePath);

            return ResponseEntity.ok(response);

        } catch (Exception e) {
            System.err.println("Upload exception: " + e.getMessage());  // Debug log
            e.printStackTrace();  // Full stack in console
            response.put("error", "Upload failed: " + e.getMessage());
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(response);
        }
    }

  @Operation(
    summary = "Generate report from DB",
    description = "Generates a report from existing DB data and exports to Excel for download."
)
@ApiResponse(responseCode = "200", description = "Report Excel file")
@GetMapping(value = "/report", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
public ResponseEntity<byte[]> generateReportFromDB(
        @RequestParam(defaultValue = "report_output.xlsx") String outputFile) {
    try {
        String tempDir = System.getProperty("java.io.tmpdir");
        String sanitizedOutput = outputFile.replaceAll("[^a-zA-Z0-9.-]", "_");
        String fullOutputPath = tempDir + sanitizedOutput;
        
        dataService.generateReport(fullOutputPath);

        byte[] fileBytes = java.nio.file.Files.readAllBytes(java.nio.file.Paths.get(fullOutputPath));

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + sanitizedOutput + "\"")
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(fileBytes);

    } catch (Exception e) {
        Map<String, Object> errorResponse = new HashMap<>();
        errorResponse.put("error", "Report generation failed: " + e.getMessage());
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
    }
}
}