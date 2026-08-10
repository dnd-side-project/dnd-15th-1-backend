package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import org.springframework.stereotype.Component;

import java.net.Inet6Address;
import java.net.InetAddress;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Locale;

@Component
final class PublicWebUrlValidator {

    private final HostAddressResolver addressResolver;

    PublicWebUrlValidator(HostAddressResolver addressResolver) {
        this.addressResolver = addressResolver;
    }

    void validate(String url) {
        URI uri = parse(url);
        String host = uri.getHost();
        if (!"https".equalsIgnoreCase(uri.getScheme())
                || uri.getUserInfo() != null
                || uri.getPort() != -1
                || !isAllowedHost(host)) {
            throw new MetadataUnavailableException();
        }
        List<InetAddress> addresses = addressResolver.resolve(host);
        if (addresses.isEmpty() || addresses.stream().anyMatch(this::isNonPublic)) {
            throw new MetadataUnavailableException();
        }
    }

    private URI parse(String url) {
        try {
            return new URI(url);
        } catch (URISyntaxException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }

    private boolean isAllowedHost(String host) {
        return "naver.me".equalsIgnoreCase(host)
                || isHostOrSubdomain(host, "naver.com")
                || isHostOrSubdomain(host, "instagram.com")
                || isHostOrSubdomain(host, "tistory.com");
    }

    private boolean isHostOrSubdomain(String host, String domain) {
        return domain.equalsIgnoreCase(host)
                || host != null && host.toLowerCase(Locale.ROOT).endsWith("." + domain);
    }

    private boolean isNonPublic(InetAddress address) {
        return address.isAnyLocalAddress()
                || address.isLoopbackAddress()
                || address.isLinkLocalAddress()
                || address.isSiteLocalAddress()
                || address.isMulticastAddress()
                || isIpv6UniqueLocal(address);
    }

    private boolean isIpv6UniqueLocal(InetAddress address) {
        if (!(address instanceof Inet6Address)) {
            return false;
        }
        int firstByte = address.getAddress()[0] & 0xFF;
        return (firstByte & 0xFE) == 0xFC;
    }
}
