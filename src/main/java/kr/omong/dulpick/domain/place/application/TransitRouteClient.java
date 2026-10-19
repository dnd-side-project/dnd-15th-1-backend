package kr.omong.dulpick.domain.place.application;

import java.math.BigDecimal;
import java.util.Optional;

public interface TransitRouteClient {

    Optional<TransitRoute> find(
            BigDecimal startLongitude,
            BigDecimal startLatitude,
            BigDecimal endLongitude,
            BigDecimal endLatitude
    );
}
