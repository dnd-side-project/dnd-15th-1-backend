package kr.omong.dulpick.domain.place.presentation.dto;

import kr.omong.dulpick.domain.place.presentation.dto.request.ManualPlaceSaveRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceConfirmRequest;
import kr.omong.dulpick.domain.place.presentation.dto.response.MemberPlaceResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceDetailResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceSearchPageResponse;
import kr.omong.dulpick.domain.place.presentation.dto.response.PlaceSearchResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceResponseContractTest {

    @Test
    void keepsAliasAndOwnershipStatusWithoutMemo() {
        assertThat(componentNames(MemberPlaceResponse.class))
                .contains("alias", "ownershipStatus", "kakaoPlaceId")
                .doesNotContain("memo");
        assertThat(componentNames(ManualPlaceSaveRequest.class))
                .contains("alias")
                .doesNotContain("memo");
        assertThat(componentNames(PlaceConfirmRequest.Selection.class))
                .contains("alias")
                .doesNotContain("memo");
    }

    @Test
    void exposesIntegratedKakaoAndSavingStateOnSearchAndDetail() {
        assertThat(componentNames(PlaceSearchResponse.class))
                .contains(
                        "placeId",
                        "kakaoPlaceId",
                        "phone",
                        "kakaoPlaceUrl",
                        "savedByMe",
                        "ownershipStatus"
                );
        assertThat(componentNames(PlaceDetailResponse.class))
                .contains(
                        "placeId",
                        "kakaoPlaceId",
                        "phone",
                        "kakaoPlaceUrl",
                        "savedByMe",
                        "ownershipStatus",
                        "savedMemberCount"
                );
        assertThat(componentNames(MemberPlaceResponse.class))
                .contains("savedMemberCount");
        assertThat(componentNames(PlaceSearchPageResponse.class))
                .contains("places", "page", "size", "hasNext");
        assertThat(componentNames(kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentPageResponse.class))
                .contains("contents", "hasNext", "popularTags");
        assertThat(componentNames(kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentResponse.PublicPlaceResponse.class))
                .contains("dateTraits")
                .doesNotContain("savedMembers");
        assertThat(componentNames(kr.omong.dulpick.domain.place.presentation.dto.response.PublicContentResponse.class))
                .contains("places")
                .doesNotContain("savedMembers");
        assertThat(kr.omong.dulpick.domain.place.application.PopularContentTags.values())
                .containsExactly("성수", "강남", "을지로");
    }

    private String[] componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);
    }
}
