package kr.omong.dulpick.domain.place.presentation.dto;

import kr.omong.dulpick.domain.place.presentation.dto.request.ManualPlaceSaveRequest;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceConfirmRequest;
import kr.omong.dulpick.domain.place.presentation.dto.response.MemberPlaceResponse;
import org.junit.jupiter.api.Test;

import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;

class PlaceResponseContractTest {

    @Test
    void keepsAliasAndOwnershipStatusWithoutMemo() {
        assertThat(componentNames(MemberPlaceResponse.class))
                .contains("alias", "ownershipStatus")
                .doesNotContain("memo");
        assertThat(componentNames(ManualPlaceSaveRequest.class))
                .contains("alias")
                .doesNotContain("memo");
        assertThat(componentNames(PlaceConfirmRequest.Selection.class))
                .contains("alias")
                .doesNotContain("memo");
    }

    private String[] componentNames(Class<?> recordType) {
        return Arrays.stream(recordType.getRecordComponents())
                .map(component -> component.getName())
                .toArray(String[]::new);
    }
}
