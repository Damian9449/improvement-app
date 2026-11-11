package com.improvement_app.googledrive.config;

import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.services.drive.Drive;
import com.google.auth.http.HttpCredentialsAdapter;
import com.google.auth.oauth2.GoogleCredentials;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/** Auto-configuration for {@link Drive}. */
@Configuration
public class GoogleDriveConfig {
    private final GoogleCredentials googleCredentials;
    private final NetHttpTransport netHttpTransport;
    private final JacksonFactory jacksonFactory;

    @Autowired
    public GoogleDriveConfig(
            GoogleCredentials googleCredentials,
            NetHttpTransport netHttpTransport,
            JacksonFactory jacksonFactory) {
        this.googleCredentials = googleCredentials;
        this.netHttpTransport = netHttpTransport;
        this.jacksonFactory = jacksonFactory;
    }

    @Bean
    public Drive googleDrive() {
        // HttpCredentialsAdapter dokleja token OAuth2 z GoogleCredentials do każdego żądania
        return new Drive.Builder(netHttpTransport, jacksonFactory, new HttpCredentialsAdapter(googleCredentials))
                .setApplicationName("ImprovementAppBackend")
                .build();
    }
}
