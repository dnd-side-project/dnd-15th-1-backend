package kr.omong.dulpick;

import kr.omong.dulpick.global.time.ServiceTime;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

import java.util.TimeZone;

@SpringBootApplication
public class DulpickApplication {

    public static void main(String[] args) {
        TimeZone.setDefault(TimeZone.getTimeZone(ServiceTime.ZONE_ID));
        SpringApplication.run(DulpickApplication.class, args);
    }

}
