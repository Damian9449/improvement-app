package com.improvement_app.googledrive.config;

import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.DriveScopes;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.util.Set;

@Configuration
public class GoogleApiClientConfig {

    @Value("${google.credentials.folder.path}")
    Resource filePath;

    private Set<String> googleOAuth2Scopes() {
        return Set.of(DriveScopes.DRIVE);
    }

    @Bean
    public GoogleCredentials googleCredentials() throws IOException {
        try (InputStream credentialsStream = filePath.getInputStream()) {
            return GoogleCredentials.fromStream(credentialsStream)
                    .createScoped(googleOAuth2Scopes());
        }
    }

    @Bean
    public NetHttpTransport netHttpTransport() throws GeneralSecurityException, IOException {
        return GoogleNetHttpTransport.newTrustedTransport();
    }

    @Bean
    public JacksonFactory jacksonFactory() {
        return JacksonFactory.getDefaultInstance();
    }

}
