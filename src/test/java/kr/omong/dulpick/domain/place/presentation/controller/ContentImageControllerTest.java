package kr.omong.dulpick.domain.place.presentation.controller;

import kr.omong.dulpick.domain.place.application.ContentImageStorageService;
import kr.omong.dulpick.domain.place.application.exception.PublicContentImageUnavailableException;
import kr.omong.dulpick.global.exception.ErrorMonitoringService;
import kr.omong.dulpick.global.exception.GlobalExceptionHandler;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.setup.MockMvcBuilders.standaloneSetup;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class ContentImageControllerTest {

    @Mock
    private ContentImageStorageService imageStorageService;

    @Mock
    private ErrorMonitoringService errorMonitoringService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = standaloneSetup(new ContentImageController(imageStorageService))
                .setControllerAdvice(new GlobalExceptionHandler(errorMonitoringService))
                .build();
    }

    @Test
    void returnsNotFoundWithoutPassingMissingPublicImageToGlobalCriticalHandler() throws Exception {
        when(imageStorageService.load("missing-image"))
                .thenThrow(new PublicContentImageUnavailableException());

        mockMvc.perform(get("/api/v1/content-images/missing-image"))
                .andExpect(status().isNotFound())
                .andExpect(content().string(""));
        verifyNoInteractions(errorMonitoringService);
    }
}
