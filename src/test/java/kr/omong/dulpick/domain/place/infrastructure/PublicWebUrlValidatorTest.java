package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import org.junit.jupiter.api.Test;

import java.net.InetAddress;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PublicWebUrlValidatorTest {

    @Test
    void acceptsAllowedHostWithPublicAddress() {
        PublicWebUrlValidator validator = validator("8.8.8.8");

        assertThatCode(() -> validator.validate("https://m.place.naver.com/place/1/home"))
                .doesNotThrowAnyException();
    }

    @Test
    void rejectsAllowedHostResolvedToPrivateAddress() {
        PublicWebUrlValidator validator = validator("10.0.0.10");

        assertThatThrownBy(() -> validator.validate("https://m.place.naver.com/place/1/home"))
                .isInstanceOf(MetadataUnavailableException.class);
    }

    @Test
    void rejectsAllowedHostWhenAnyResolvedAddressIsLoopback() {
        HostAddressResolver resolver = host -> List.of(
                address("8.8.8.8"),
                address("127.0.0.1")
        );
        PublicWebUrlValidator validator = new PublicWebUrlValidator(resolver);

        assertThatThrownBy(() -> validator.validate("https://naver.me/example"))
                .isInstanceOf(MetadataUnavailableException.class);
    }

    private PublicWebUrlValidator validator(String ip) {
        return new PublicWebUrlValidator(host -> List.of(address(ip)));
    }

    private InetAddress address(String ip) {
        try {
            return InetAddress.getByName(ip);
        } catch (Exception exception) {
            throw new IllegalStateException(exception);
        }
    }
}
