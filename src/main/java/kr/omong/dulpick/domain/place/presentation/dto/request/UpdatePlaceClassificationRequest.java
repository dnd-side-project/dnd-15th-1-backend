package kr.omong.dulpick.domain.place.presentation.dto.request;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.fasterxml.jackson.annotation.JsonSetter;
import io.swagger.v3.oas.annotations.media.Schema;
import kr.omong.dulpick.domain.place.domain.PlaceActivity;
import kr.omong.dulpick.domain.place.domain.PlaceEnvironment;
import kr.omong.dulpick.domain.place.domain.PlaceFocus;
import kr.omong.dulpick.domain.place.domain.PlaceTime;

@Schema(description = "변경할 축만 보냅니다. 필드를 생략하면 유지하고, null이면 해당 축을 비웁니다.")
public class UpdatePlaceClassificationRequest {

    private PlaceEnvironment environment;
    private boolean environmentPresent;
    private PlaceActivity activity;
    private boolean activityPresent;
    private PlaceTime time;
    private boolean timePresent;
    private PlaceFocus focus;
    private boolean focusPresent;

    @Schema(
            description = "실내·실외. 생략 시 유지, null이면 비움",
            allowableValues = {"INDOOR", "OUTDOOR"},
            example = "INDOOR",
            nullable = true
    )
    public PlaceEnvironment getEnvironment() {
        return environment;
    }

    @JsonSetter("environment")
    public void setEnvironment(PlaceEnvironment environment) {
        this.environment = environment;
        this.environmentPresent = true;
    }

    @Schema(
            description = "액티비티·정적. 생략 시 유지, null이면 비움",
            allowableValues = {"ACTIVE", "STATIC"},
            example = "STATIC",
            nullable = true
    )
    public PlaceActivity getActivity() {
        return activity;
    }

    @JsonSetter("activity")
    public void setActivity(PlaceActivity activity) {
        this.activity = activity;
        this.activityPresent = true;
    }

    @Schema(
            description = "낮·밤. 생략 시 유지, null이면 비움",
            allowableValues = {"DAY", "NIGHT"},
            example = "DAY",
            nullable = true
    )
    public PlaceTime getTime() {
        return time;
    }

    @JsonSetter("time")
    public void setTime(PlaceTime time) {
        this.time = time;
        this.timePresent = true;
    }

    @Schema(
            description = "식사·볼거리. 생략 시 유지, null이면 비움",
            allowableValues = {"FOOD", "SIGHTSEEING"},
            example = "FOOD",
            nullable = true
    )
    public PlaceFocus getFocus() {
        return focus;
    }

    @JsonSetter("focus")
    public void setFocus(PlaceFocus focus) {
        this.focus = focus;
        this.focusPresent = true;
    }

    @JsonIgnore
    public boolean isEnvironmentPresent() {
        return environmentPresent;
    }

    @JsonIgnore
    public boolean isActivityPresent() {
        return activityPresent;
    }

    @JsonIgnore
    public boolean isTimePresent() {
        return timePresent;
    }

    @JsonIgnore
    public boolean isFocusPresent() {
        return focusPresent;
    }

    @JsonIgnore
    public boolean hasAnyChange() {
        return environmentPresent || activityPresent || timePresent || focusPresent;
    }
}
