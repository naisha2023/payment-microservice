package org.example.authservice;

import org.springframework.boot.ApplicationRunner;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.core.env.Environment;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.example.authservice",
        "org.example.shared"
})
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = {
        "org.example.authservice.repository",
        "org.example.shared.repository"
})
@EntityScan(basePackages = {
        "org.example.authservice.entity",
        "org.example.shared.entity"
})

public class AuthServiceApplication {

@Bean
ApplicationRunner runner(Environment env) {
    return args -> {
        System.out.println("ssl.password=[" +
                env.getProperty("ssl.password") + "]");
        System.out.println("SSL_PASSWORD=[" +
                env.getProperty("SSL_PASSWORD") + "]");
    };
}

    public static void main(String[] args) {
        SpringApplication.run(AuthServiceApplication.class, args);
    }

}
