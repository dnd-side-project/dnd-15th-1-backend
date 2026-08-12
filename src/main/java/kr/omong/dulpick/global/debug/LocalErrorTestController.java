package kr.omong.dulpick.global.debug;

import org.springframework.context.annotation.Profile;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Profile("local")
public class LocalErrorTestController {

    @GetMapping(value = "/health", params = "criticalError=true")
    public void triggerCriticalError(@RequestParam boolean criticalError) {
        if (criticalError) {
            throw new RuntimeException("Critical Error 알림 테스트");
        }
    }
}
