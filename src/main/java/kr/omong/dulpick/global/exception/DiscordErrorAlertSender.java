package kr.omong.dulpick.global.exception;

import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;

import java.util.Map;

@Component
@RequiredArgsConstructor
public class DiscordErrorAlertSender implements ErrorAlertSender {

    private final ErrorDiscordProperties properties;

    @Override
    public void sendCriticalAlert(String message) {
        if (!properties.isEnabled() || properties.getWebhookUrl().isBlank()) {
            return;
        }

        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getConnectTimeout());
        requestFactory.setReadTimeout(properties.getReadTimeout());

        RestClient restClient = RestClient.builder()
                .requestFactory(requestFactory)
                .build();

        restClient.post()
                .uri(properties.getWebhookUrl())
                .contentType(MediaType.APPLICATION_JSON)
                .body(Map.of("content", message))
                .retrieve()
                .toBodilessEntity();
    }
}
