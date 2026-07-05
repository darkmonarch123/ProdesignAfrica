package africa.prodesign;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class ProdesignAfricaApplication {
    public static void main(String[] args) {
        SpringApplication.run(ProdesignAfricaApplication.class, args);
    }
}
