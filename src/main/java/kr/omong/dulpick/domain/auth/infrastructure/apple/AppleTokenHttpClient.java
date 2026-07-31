package kr.omong.dulpick.domain.auth.infrastructure.apple;

import org.springframework.http.MediaType;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

public class AppleTokenHttpClient implements AppleTokenClient {

    private final AppleTokenProperties properties;
    private final AppleClientSecretGenerator clientSecretGenerator;
    private final RestClient restClient;

    public AppleTokenHttpClient(
            AppleTokenProperties properties,
            AppleClientSecretGenerator clientSecretGenerator,
            RestClient restClient
    ) {
        this.properties = properties;
        this.clientSecretGenerator = clientSecretGenerator;
        this.restClient = restClient;
    }

    @Override
    public AppleTokenResponse exchange(String authorizationCode, String clientId) {
        MultiValueMap<String, String> form = commonForm(clientId);
        form.add("grant_type", "authorization_code");
        form.add("code", authorizationCode);
        try {
            return restClient.post()
                    .uri(properties.tokenUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .body(AppleTokenResponse.class);
        } catch (RestClientException exception) {
            throw new AppleAuthorizationException("Apple authorization code exchange failed", exception);
        }
    }

    @Override
    public void revoke(String refreshToken, String clientId) {
        MultiValueMap<String, String> form = commonForm(clientId);
        form.add("token", refreshToken);
        form.add("token_type_hint", "refresh_token");
        try {
            restClient.post()
                    .uri(properties.revokeUri())
                    .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                    .body(form)
                    .retrieve()
                    .toBodilessEntity();
        } catch (RestClientException exception) {
            throw new AppleAuthorizationException("Apple token revocation failed", exception);
        }
    }

    private MultiValueMap<String, String> commonForm(String clientId) {
        MultiValueMap<String, String> form = new LinkedMultiValueMap<>();
        form.add("client_id", clientId);
        form.add("client_secret", clientSecretGenerator.generate(clientId));
        return form;
    }
}
