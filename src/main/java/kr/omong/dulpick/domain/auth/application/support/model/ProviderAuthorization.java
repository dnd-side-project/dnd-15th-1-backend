package kr.omong.dulpick.domain.auth.application.support.model;

public record ProviderAuthorization(
        String encryptedRefreshToken,
        String clientId
) {

    public static ProviderAuthorization none() {
        return new ProviderAuthorization(null, null);
    }

    public static ProviderAuthorization clientIdOnly(String clientId) {
        return new ProviderAuthorization(null, clientId);
    }

    public boolean hasRefreshToken() {
        return encryptedRefreshToken != null;
    }

    public boolean hasClientId() {
        return clientId != null;
    }
}
