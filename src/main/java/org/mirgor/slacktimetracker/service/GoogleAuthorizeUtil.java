package org.mirgor.slacktimetracker.service;

import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.sheets.v4.SheetsScopes;
import com.google.auth.oauth2.GoogleCredentials;
import com.google.auth.oauth2.ServiceAccountCredentials;

import java.io.IOException;
import java.io.InputStream;
import java.util.Collections;
import java.util.List;

public class GoogleAuthorizeUtil {

    private static final List<String> SCOPES = Collections.singletonList(SheetsScopes.SPREADSHEETS);
    private static final String SERVICE_ACCOUNT_FILE_PATH = "/crack-mix-425415-r2-0e1afc85995b.json";


    public static GoogleCredentials getCredentials() throws IOException {
        InputStream in = GoogleAuthorizeUtil.class.getResourceAsStream(SERVICE_ACCOUNT_FILE_PATH);
        return ServiceAccountCredentials.fromStream(in).createScoped(SCOPES);
    }

}