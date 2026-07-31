package kr.omong.dulpick.global.config;

import kr.omong.dulpick.global.time.ServiceTime;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Clock;

@Configuration
public class TimeConfig {

    @Bean
    public Clock clock() {
        return Clock.system(ServiceTime.ZONE_ID);
    }
}
