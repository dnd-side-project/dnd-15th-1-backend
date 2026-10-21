package kr.omong.dulpick.domain.place.infrastructure;

import kr.omong.dulpick.domain.place.application.RoutePoint;

import java.util.ArrayList;
import java.util.List;

final class KakaoRoutePathParser {

    private KakaoRoutePathParser() {
    }

    /**
     * 도보 응답의 route.legs[].steps[].path.points를 이어 붙입니다.
     * 구간 경계의 중복 좌표는 하나로 합칩니다. legs가 없으면 route.path를 사용합니다.
     */
    static List<RoutePoint> parseWalkingRoute(java.util.Map<?, ?> route) {
        List<RoutePoint> path = new ArrayList<>();
        if (route.get("legs") instanceof List<?> legs) {
            for (Object legValue : legs) {
                if (!(legValue instanceof java.util.Map<?, ?> leg) || !(leg.get("steps") instanceof List<?> steps)) {
                    continue;
                }
                for (Object stepValue : steps) {
                    if (stepValue instanceof java.util.Map<?, ?> step
                            && step.get("path") instanceof java.util.Map<?, ?> stepPath) {
                        for (RoutePoint point : parse(stepPath.get("points"))) {
                            if (path.isEmpty() || !path.getLast().equals(point)) {
                                path.add(point);
                            }
                        }
                    }
                }
            }
        }
        return path.isEmpty() ? parse(route.get("path")) : List.copyOf(path);
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
