package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.RoutePoint;

import java.util.ArrayList;
import java.util.List;

final class KakaoRoutePathParser {

    private KakaoRoutePathParser() {
    }

    static List<RoutePoint> parse(Object pathValue) {
        if (!(pathValue instanceof List<?> items)) {
            return List.of();
        }
        List<RoutePoint> path = new ArrayList<>(items.size());
        for (Object item : items) {
            RoutePoint point = point(item);
            if (point != null) {
                path.add(point);
            }
        }
        return List.copyOf(path);
    }

    private static RoutePoint point(Object item) {
        if (item instanceof java.util.Map<?, ?> map) {
            return point(map.get("x"), map.get("y"));
        }
        if (item instanceof List<?> pair && pair.size() >= 2) {
            return point(pair.get(0), pair.get(1));
        }
        return null;
    }

    private static RoutePoint point(Object longitude, Object latitude) {
        Double x = number(longitude);
        Double y = number(latitude);
        return x == null || y == null ? null : new RoutePoint(y, x);
    }

    private static Double number(Object value) {
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        if (value == null) {
            return null;
        }
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException exception) {
            return null;
        }
    }
}
