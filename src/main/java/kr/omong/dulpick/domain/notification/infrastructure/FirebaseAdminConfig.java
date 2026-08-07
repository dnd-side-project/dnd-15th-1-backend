package kr.omong.dulpick.domain.notification.infrastructure;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.FirebaseMessaging;
import kr.omong.dulpick.domain.notification.config.FcmProperties;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;

@Configuration
@ConditionalOnProperty(name = "notification.fcm.enabled", havingValue = "true")
public class FirebaseAdminConfig {

    private static final String APP_NAME = "dulpick-notification";

    @Bean(destroyMethod = "delete")
    public FirebaseApp notificationFirebaseApp(
            FcmProperties properties,
            PushRegistrationCipher registrationCipher
    ) throws IOException {
        if (properties.projectId() == null || properties.projectId().isBlank()) {
            throw new IllegalStateException("FIREBASE_PROJECT_ID is required");
        }
        registrationCipher.requireConfigured();
        FirebaseOptions options = FirebaseOptions.builder()
                .setCredentials(loadCredentials(properties))
                .setProjectId(properties.projectId())
                .build();
        return FirebaseApp.initializeApp(options, APP_NAME);
    }

    @Bean
    public FirebaseMessaging firebaseMessaging(FirebaseApp notificationFirebaseApp) {
        return FirebaseMessaging.getInstance(notificationFirebaseApp);
    }

    private GoogleCredentials loadCredentials(FcmProperties properties) throws IOException {
        if (properties.credentialsPath() == null
                || properties.credentialsPath().isBlank()) {
            return GoogleCredentials.getApplicationDefault();
        }
        try (InputStream stream = Files.newInputStream(
                Path.of(properties.credentialsPath())
        )) {
            return GoogleCredentials.fromStream(stream);
        }
    }
}
