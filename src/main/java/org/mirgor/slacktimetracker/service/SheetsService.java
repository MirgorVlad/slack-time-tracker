package org.mirgor.slacktimetracker.service;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.Sheets;
import com.google.api.services.sheets.v4.model.Spreadsheet;
import com.google.api.services.sheets.v4.model.ValueRange;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.mirgor.slacktimetracker.service.dto.Headers;
import org.mirgor.slacktimetracker.service.dto.TimeInfo;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.*;

@Slf4j
@Service
public class SheetsService {

    private static final String APPLICATION_NAME = "Time-tracker";
    private Map<Headers, Integer> HEADERS;

    private Sheets sheetsService;

    @PostConstruct
    void init() throws GeneralSecurityException, IOException {
        GoogleCredentials credential = GoogleAuthorizeUtil.getCredentials();
        sheetsService = new Sheets.Builder(
                GoogleNetHttpTransport.newTrustedTransport(),
                JacksonFactory.getDefaultInstance(), new HttpCredentialsAdapter(credential))
                .setApplicationName(APPLICATION_NAME)
                .build();

        HEADERS = new TreeMap<>();
        HEADERS.put(Headers.PROJECT, 0);
        HEADERS.put(Headers.FE, 1);
        HEADERS.put(Headers.BE, 2);
        HEADERS.put(Headers.FE_FIX, 3);
        HEADERS.put(Headers.BE_FIX, 4);
    }

    public void addTime(String spreadsheetId, List<TimeInfo> timeInfoList) throws IOException {
        // 1. Get the first sheet's name
        Spreadsheet spreadsheet = sheetsService.spreadsheets().get(spreadsheetId).execute();
        if (spreadsheet.getSheets() == null || spreadsheet.getSheets().isEmpty())
            throw new RuntimeException("No sheets found.");

        String sheetName = spreadsheet.getSheets().get(0).getProperties().getTitle();
        String headerRange = sheetName + "!1:1";
        ValueRange headersResp = sheetsService.spreadsheets().values()
                .get(spreadsheetId, headerRange)
                .execute();

        List<Object> headers = getHeaders(headersResp);
        updateHeaders(spreadsheetId, headers, headerRange);

        // 3. Read all data rows (including header, for 1-based calc)
        String wholeRange = String.format("%s!A1:%s", sheetName,
                (char) ('A' + headers.size() - 1));
        List<List<Object>> rows = sheetsService.spreadsheets().values()
                .get(spreadsheetId, wholeRange)
                .execute()
                .getValues();
        if (rows == null || rows.isEmpty()) rows = new ArrayList<>();


        // 4. Find a row where [projCol] equals name
        Integer projCol = HEADERS.get(Headers.PROJECT);

        for (TimeInfo timeInfo : timeInfoList) {
            int rowIdx = -1;
            for (int i = 1; i < rows.size(); ++i) {
                List<Object> row = rows.get(i);
                if (projCol < row.size() && row.get(projCol).toString().equalsIgnoreCase(timeInfo.project())) {
                    rowIdx = i; // 0-based index, including header row
                    break;
                }
            }
            if (rowIdx != -1) {
                updateTime(spreadsheetId, rows, rowIdx, sheetName, timeInfo);
            } else {
                List<Object> newRow = createTime(spreadsheetId, headers, projCol, sheetName, timeInfo);
                rows.add(newRow);
            }
        }
    }

    private Double getValue(Headers header, TimeInfo timeInfo) {
        boolean bugfix = timeInfo.bugfix();
        return switch (header) {
            case FE -> bugfix ? 0 : timeInfo.fe();
            case BE -> bugfix ? 0 : timeInfo.be();
            case FE_FIX -> bugfix ? timeInfo.fe() : 0;
            case BE_FIX -> bugfix ? timeInfo.be() : 0;
            default -> 0.;
        };

    }

    private List<Object> createTime(String spreadsheetId, List<Object> headers,
                                    Integer projCol, String sheetName, TimeInfo timeInfo) throws IOException {
        double total = 0;
        List<Object> newRow = new ArrayList<>(Collections.nCopies(headers.size(), ""));
        newRow.set(projCol, timeInfo.project());
        for (Headers header : Headers.values()) {
            if (header.equals(Headers.PROJECT)) {
                continue;
            }
            Integer timeCol = HEADERS.get(header);
            Double value = getValue(header, timeInfo);
            total += value;
            newRow.set(timeCol, value);
        }
        newRow.set(HEADERS.size(), total);
        ValueRange appendBody = new ValueRange().setValues(List.of(newRow));
        sheetsService.spreadsheets().values()
                .append(spreadsheetId, sheetName, appendBody)
                .setValueInputOption("RAW")
                .setInsertDataOption("INSERT_ROWS")
                .execute();
        return newRow;
    }

    private void updateTimeV2(String spreadsheetId, List<Object> headers,
                            int projCol,
                            List<List<Object>> rows, int rowIdx, String sheetName, TimeInfo timeInfo) throws IOException {
        double total = 0;
        List<Object> newRow = new ArrayList<>(Collections.nCopies(headers.size(), ""));
        List<Object> row = rows.get(rowIdx);
        newRow.set(projCol, timeInfo.project());
        for (Headers header : Headers.values()) {
            if (header.equals(Headers.PROJECT)) {
                continue;
            }
            Integer timeCol = HEADERS.get(header);
            Double oldValue = Double.parseDouble(row.get(timeCol).toString());
            Double newValue = oldValue + getValue(header, timeInfo);
            total += newValue;
            newRow.set(timeCol, newValue);
        }
        newRow.set(headers.size(), total);
        ValueRange updateBody = new ValueRange().setValues(List.of(newRow));
        String cell = String.format("%s!%s%d", sheetName, (char) ('A' + rowIdx), headers.size());
        sheetsService.spreadsheets().values()
                .update(spreadsheetId, cell, updateBody)
                .setValueInputOption("RAW")
                .execute();
    }

    private void updateTime(String spreadsheetId,
                            List<List<Object>> rows, int rowIdx, String sheetName, TimeInfo timeInfo) throws IOException {
        double total = 0;
        List<Object> row = rows.get(rowIdx);
        for (Headers header : Headers.values()) {
            if (header.equals(Headers.PROJECT)) {
                continue;
            }
            Integer timeCol = HEADERS.get(header);
            Double value = getValue(header, timeInfo);
            double current = Double.parseDouble(row.get(timeCol).toString());
            double newValue = current + value;
            total += newValue;
            String cell = String.format("%s!%s%d", sheetName, (char) ('A' + timeCol), rowIdx + 1);
            ValueRange updateBody = new ValueRange().setValues(List.of(List.of(newValue)));
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, cell, updateBody)
                    .setValueInputOption("RAW")
                    .execute();
        }
    }

    private static List<Object> getHeaders(ValueRange headersResp) {
        return (headersResp.getValues() != null && !headersResp.getValues().isEmpty())
                ? headersResp.getValues().get(0)
                : new ArrayList<>();
    }

    private void updateHeaders(String spreadsheetId, List<Object> headers, String headerRange) throws IOException {
        // 2. Ensure "Project" and "time" headers exist
        boolean needUpdateHeaders = false;
        for (Headers header : HEADERS.keySet()) {
            if (!headers.contains(header.getHeader())) {
                headers.add(header.getHeader());
                needUpdateHeaders = true;
            }
        }

        if (needUpdateHeaders) {
            ValueRange headerBody = new ValueRange().setValues(List.of(headers));
            sheetsService.spreadsheets().values()
                    .update(spreadsheetId, headerRange, headerBody)
                    .setValueInputOption("RAW")
                    .execute();
        }
        // Now, refresh the indices in case columns were added
        headers.forEach(headers::indexOf);
    }
}