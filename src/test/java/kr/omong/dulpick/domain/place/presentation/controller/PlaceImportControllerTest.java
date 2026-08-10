package kr.omong.dulpick.domain.place.presentation.controller;

import kr.omong.dulpick.domain.place.application.PlaceCommandService;
import kr.omong.dulpick.domain.place.application.PlaceImportNextAction;
import kr.omong.dulpick.domain.place.application.PlaceImportService;
import kr.omong.dulpick.domain.place.application.PlaceImportSubmissionView;
import kr.omong.dulpick.domain.place.application.PlaceImportView;
import kr.omong.dulpick.domain.place.domain.ContentSourceType;
import kr.omong.dulpick.domain.place.domain.PlaceImportStatus;
import kr.omong.dulpick.domain.place.presentation.dto.request.PlaceImportRequest;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.oauth2.jwt.Jwt;

import java.time.Instant;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class PlaceImportControllerTest {

    private final PlaceImportService importService = mock(PlaceImportService.class);
    private final PlaceImportController controller = new PlaceImportController(
            importService,
            mock(PlaceCommandService.class)
    );
    private final Jwt jwt = mock(Jwt.class);

    @Test
    void returnsAcceptedWithPollingLocationWhileAnalysisIsPending() {
        when(jwt.getSubject()).thenReturn("1");
        when(importService.importLink(1L, "https://www.instagram.com/reel/example"))
                .thenReturn(new PlaceImportSubmissionView(
                        view(PlaceImportStatus.RECEIVED, PlaceImportNextAction.WAIT)
                ));

        var response = controller.importContent(
                jwt,
                new PlaceImportRequest("https://www.instagram.com/reel/example")
        );

        assertThat(response.getStatusCode()).isEqualTo(HttpStatus.ACCEPTED);
        assertThat(response.getHeaders().getLocation())
                .hasToString("/api/v1/place-imports/10");
        assertThat(response.getBody().retryAfterSeconds()).isEqualTo(5L);
    }

    @Test
    void returnsOkForCachedCompletedImport() {
        when(jwt.getSubject()).thenReturn("1");
        PlaceImportView view = view(
                PlaceImportStatus.REVIEW_REQUIRED,
                PlaceImportNextAction.SELECT_PLACES
        );
        when(importService.importLink(1L, "existing"))
                .thenReturn(new PlaceImportSubmissionView(view));

        assertThat(controller.importContent(jwt, new PlaceImportRequest("existing")).getStatusCode())
                .isEqualTo(HttpStatus.OK);
    }

    private PlaceImportView view(
            PlaceImportStatus status,
            PlaceImportNextAction nextAction
    ) {
        Instant now = Instant.parse("2026-08-10T00:00:00Z");
        return new PlaceImportView(
                10L,
                20L,
                "https://www.instagram.com/reel/example",
                ContentSourceType.INSTAGRAM_REEL,
                status,
                nextAction,
                status == PlaceImportStatus.RECEIVED ? 5L : null,
                null,
                new PlaceImportView.ContentView(null, null, null, null, null, null),
                now,
                now,
                List.of()
        );
    }
}
