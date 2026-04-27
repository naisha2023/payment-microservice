package org.example.paymentservice;

import org.springframework.amqp.rabbit.annotation.EnableRabbit;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.scheduling.annotation.EnableScheduling;

@EnableFeignClients
@EnableRabbit
@EnableScheduling
@EnableAspectJAutoProxy
@SpringBootApplication(scanBasePackages = {
        "org.example.paymentservice",
        "org.example.shared"
})
@EnableJpaRepositories(basePackages = {
        "org.example.paymentservice.repository",
        "org.example.shared.repository"
})
@EntityScan(basePackages = {
        "org.example.paymentservice.entity",
        "org.example.shared.entity"
})
public class PaymentServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(PaymentServiceApplication.class, args);
    }

}
