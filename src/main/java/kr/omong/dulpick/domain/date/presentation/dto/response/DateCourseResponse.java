package kr.omong.dulpick.domain.date.presentation.dto.response;

import io.swagger.v3.oas.annotations.media.ArraySchema;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.date.application.query.view.DateCoursePlaceView;
import kr.omong.dulpick.domain.date.application.query.view.DateCourseView;
import kr.omong.dulpick.domain.date.domain.DateCourseStatus;
import kr.omong.dulpick.domain.place.application.RoutePoint;
import kr.omong.dulpick.domain.place.application.TransitLeg;
import kr.omong.dulpick.domain.place.application.TransitRoute;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceDateTraitsResponse;
import kr.omong.dulpick.domain.place.application.WalkingRoute;
import kr.omong.dulpick.global.time.ServiceTime;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.List;

public record DateCourseResponse(
        @Schema(example = "1001")
        Long dateCourseId,
        @Schema(example = "성수동 데이트")
        String title,
        @Schema(description = "데이트 날짜(Asia/Seoul)", example = "2026-08-16")
        LocalDate date,
        @Schema(
                description = "데이트 시간(Asia/Seoul). 날짜만 저장된 경우 null입니다.",
                example = "19:30:00",
                nullable = true,
                requiredMode = Schema.RequiredMode.NOT_REQUIRED
        )
        LocalTime time,
        @Schema(example = "DRAFT")
        DateCourseStatus status,
        @Schema(example = "0")
        long version,
        @Schema(example = "2")
        int totalPlaceCount,
        @Schema(example = "[]")
        List<DateCoursePlaceResponse> places
) {

    public static DateCourseResponse from(DateCourseView view) {
        LocalDateTime scheduledAt = ServiceTime.toLocalDateTime(view.scheduledAt());
        return new DateCourseResponse(
                view.dateCourseId(),
                view.title(),
                scheduledAt == null ? null : scheduledAt.toLocalDate(),
                ServiceTime.toResponseTime(scheduledAt),
                view.status(),
                view.version(),
                view.totalPlaceCount(),
                view.places().stream().map(DateCoursePlaceResponse::from).toList()
        );
    }

    public record DateCoursePlaceResponse(
            @Schema(description = "데이트 코스 내 장소 순서", example = "1")
            int order,
            @Schema(example = "101")
            Long placeId,
            @Schema(example = "서울숲 카페")
            String name,
            @Schema(example = "서울특별시 성동구 성수동1가 685-700")
            String address,
            @Schema(example = "서울특별시 성동구 서울숲2길 10")
            String roadAddress,
            @Schema(example = "37.5446")
            BigDecimal latitude,
            @Schema(example = "127.0557")
            BigDecimal longitude,
            @Schema(description = "Kakao 원본 카테고리 경로", example = "음식점 > 카페")
            String category,
            @Schema(description = "둘픽 장소 분류", example = "카페")
            String categoryName,
            @Schema(example = "https://example.com/place.jpg")
            String thumbnailUrl,
            @ArraySchema(schema = @Schema(example = "https://example.com/place-detail.jpg"))
            @Schema(example = "[]")
            List<String> imageUrls,
            @Schema(
                    description = "다음 장소까지 도보 이동 정보. 마지막 장소이거나 경로를 조회하지 못하면 null입니다.",
                    nullable = true,
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            WalkToNextResponse walkToNext,
            @Schema(
                    description = "장소에 저장된 네 축 분류(실내·실외, 액티비티·정적, 낮·밤, 식사·볼거리). 미분류 장소는 status가 UNCLASSIFIED이고 각 축은 null입니다.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            PlaceDateTraitsResponse dateTraits
    ) {

        private static DateCoursePlaceResponse from(DateCoursePlaceView view) {
            return new DateCoursePlaceResponse(
                    view.order(),
                    view.placeId(),
                    view.name(),
                    view.address(),
                    view.roadAddress(),
                    view.latitude(),
                    view.longitude(),
                    view.category(),
                    view.categoryName(),
                    view.thumbnailUrl(),
                    view.imageUrls(),
                    WalkToNextResponse.from(view.walkToNext()),
                    PlaceDateTraitsResponse.from(view.dateTraits())
            );
        }
    }

    public record WalkToNextResponse(
            @Schema(description = "다음 장소까지 도보 이동거리(미터)", example = "4025", requiredMode = Schema.RequiredMode.REQUIRED)
            int distanceMeters,
            @Schema(description = "다음 장소까지 도보 이동시간(초)", example = "3914", requiredMode = Schema.RequiredMode.REQUIRED)
            int durationSeconds,
            @ArraySchema(schema = @Schema(implementation = RoutePointResponse.class))
            @Schema(
                    description = "도보 경로를 따라가는 좌표 목록. 경로 좌표를 받지 못하면 빈 배열입니다.",
                    requiredMode = Schema.RequiredMode.REQUIRED
            )
            List<RoutePointResponse> path,
            @Schema(
                    description = "도보 이동시간이 30분 이상일 때 함께 제공하는 대중교통 경로. 해당하지 않거나 조회하지 못하면 null입니다.",
                    nullable = true,
                    requiredMode = Schema.RequiredMode.NOT_REQUIRED
            )
            TransitToNextResponse transit
    ) {

        private static WalkToNextResponse from(WalkingRoute route) {
            return route == null ? null : new WalkToNextResponse(
                    route.distanceMeters(),
                    route.durationSeconds(),
                    RoutePointResponse.fromAll(route.path()),
                    TransitToNextResponse.from(route.transit())
            );
        }
    }

    public record TransitToNextResponse(
            @Schema(description = "대중교통 경로 유형", allowableValues = {"BUS", "SUBWAY", "BUS_AND_SUBWAY"}, example = "BUS_AND_SUBWAY", requiredMode = Schema.RequiredMode.REQUIRED)
            String type,
            @Schema(description = "대중교통 총 이동거리(미터)", example = "5200", requiredMode = Schema.RequiredMode.REQUIRED)
            int distanceMeters,
            @Schema(description = "대중교통 총 이동시간(초)", example = "2700", requiredMode = Schema.RequiredMode.REQUIRED)
            int durationSeconds,
            @Schema(description = "환승 횟수", example = "1", requiredMode = Schema.RequiredMode.REQUIRED)
            int transfers,
            @Schema(description = "요금(원). 제공되지 않으면 null입니다.", example = "1550", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            Integer fare,
            @ArraySchema(schema = @Schema(implementation = TransitLegResponse.class))
            @Schema(description = "도보/버스/지하철 구간 목록(이동 순서)", requiredMode = Schema.RequiredMode.REQUIRED)
            List<TransitLegResponse> legs
    ) {

        private static TransitToNextResponse from(TransitRoute route) {
            return route == null ? null : new TransitToNextResponse(
                    route.type(),
                    route.distanceMeters(),
                    route.durationSeconds(),
                    route.transfers(),
                    route.fare(),
                    route.legs().stream().map(TransitLegResponse::from).toList()
            );
        }
    }

    public record TransitLegResponse(
            @Schema(description = "구간 이동 수단", allowableValues = {"WALKING", "BUS", "SUBWAY"}, example = "SUBWAY", requiredMode = Schema.RequiredMode.REQUIRED)
            String type,
            @Schema(description = "안내 문구", example = "2호선 (성수 > 강남)", requiredMode = Schema.RequiredMode.REQUIRED)
            String guidance,
            @Schema(description = "구간 이동거리(미터)", example = "4100", requiredMode = Schema.RequiredMode.REQUIRED)
            int distanceMeters,
            @Schema(description = "구간 이동시간(초)", example = "1800", requiredMode = Schema.RequiredMode.REQUIRED)
            int durationSeconds,
            @Schema(description = "노선/버스 번호. 도보 구간이면 null입니다.", example = "2호선", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String vehicleName,
            @Schema(description = "차량 세부 종류(간선, 지선 등). 값은 변경될 수 있습니다.", example = "간선", nullable = true, requiredMode = Schema.RequiredMode.NOT_REQUIRED)
            String vehicleType,
            @ArraySchema(schema = @Schema(example = "성수"))
            @Schema(description = "정류장/역 이름 목록", requiredMode = Schema.RequiredMode.REQUIRED)
            List<String> stops,
            @ArraySchema(schema = @Schema(implementation = RoutePointResponse.class))
            @Schema(description = "구간 경로 좌표 목록", requiredMode = Schema.RequiredMode.REQUIRED)
            List<RoutePointResponse> path
    ) {

        private static TransitLegResponse from(TransitLeg leg) {
            return new TransitLegResponse(
                    leg.type(),
                    leg.guidance(),
                    leg.distanceMeters(),
                    leg.durationSeconds(),
                    leg.vehicleName(),
                    leg.vehicleType(),
                    leg.stops(),
                    RoutePointResponse.fromAll(leg.path())
            );
        }
    }

    public record RoutePointResponse(
            @Schema(description = "WGS84 위도", example = "37.5446", requiredMode = Schema.RequiredMode.REQUIRED)
            double latitude,
            @Schema(description = "WGS84 경도", example = "127.0557", requiredMode = Schema.RequiredMode.REQUIRED)
            double longitude
    ) {

        private static List<RoutePointResponse> fromAll(List<RoutePoint> path) {
            return path.stream()
                    .map(point -> new RoutePointResponse(point.latitude(), point.longitude()))
                    .toList();
        }
    }
}
