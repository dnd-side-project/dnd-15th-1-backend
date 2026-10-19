package kr.omong.dulpick.domain.place.application;

import java.util.ArrayList;
import java.util.List;

/**
 * 경로 좌표를 캐시 컬럼에 저장하기 위한 "lat,lng;lat,lng" 형식 변환기입니다.
 */
public final class RoutePathCodec {

    private RoutePathCodec() {
    }

    public static String encode(List<RoutePoint> path) {
        if (path == null || path.isEmpty()) {
            return null;
        }
        StringBuilder builder = new StringBuilder();
        for (RoutePoint point : path) {
            if (!builder.isEmpty()) {
                builder.append(';');
            }
            builder.append(point.latitude()).append(',').append(point.longitude());
        }
        return builder.toString();
    }

    public static List<RoutePoint> decode(String encoded) {
        if (encoded == null || encoded.isBlank()) {
            return List.of();
        }
        List<RoutePoint> path = new ArrayList<>();
        for (String pair : encoded.split(";")) {
            String[] values = pair.split(",");
            if (values.length != 2) {
                return List.of();
            }
            try {
                path.add(new RoutePoint(Double.parseDouble(values[0]), Double.parseDouble(values[1])));
            } catch (NumberFormatException exception) {
                return List.of();
            }
        }
        return List.copyOf(path);
    }
}
