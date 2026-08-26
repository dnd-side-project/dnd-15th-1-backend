package kr.omong.dulpick.domain.place.infrastructure;

import java.net.InetAddress;
import java.util.List;

interface HostAddressResolver {

    List<InetAddress> resolve(String host);
}
