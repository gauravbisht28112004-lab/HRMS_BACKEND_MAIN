package com.financebuddha.finbud.hrms;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.data.jpa.repository.config.EnableJpaAuditing;
import org.springframework.scheduling.annotation.EnableAsync;
import org.springframework.scheduling.annotation.EnableScheduling;

import java.util.TimeZone;

@SpringBootApplication
@EnableJpaAuditing
@EnableScheduling
@EnableAsync
@EnableCaching
public class FinbudHrmsApplication {

    public static void main(String[] args) {
        // Force JVM timezone to IST so all LocalDateTime.now() / LocalDate.now()
        // calls across the application produce Indian Standard Time (UTC+5:30)
        // regardless of the server's OS timezone (typically UTC on cloud/Docker).
        TimeZone.setDefault(TimeZone.getTimeZone("Asia/Kolkata"));
        SpringApplication.run(FinbudHrmsApplication.class, args);
    }
}
