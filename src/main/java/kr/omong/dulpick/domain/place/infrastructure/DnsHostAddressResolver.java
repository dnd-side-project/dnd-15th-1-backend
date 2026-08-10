package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.exception.MetadataUnavailableException;
import org.springframework.stereotype.Component;

import java.net.InetAddress;
import java.net.UnknownHostException;
import java.util.Arrays;
import java.util.List;

@Component
final class DnsHostAddressResolver implements HostAddressResolver {

    @Override
    public List<InetAddress> resolve(String host) {
        try {
            return Arrays.asList(InetAddress.getAllByName(host));
        } catch (UnknownHostException exception) {
            throw new MetadataUnavailableException(exception);
        }
    }
}
