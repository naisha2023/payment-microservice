package org.example.walletservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.persistence.autoconfigure.EntityScan;
import org.springframework.context.annotation.EnableAspectJAutoProxy;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

@SpringBootApplication(scanBasePackages = {
        "org.example.walletservice",
        "org.example.shared"
})
@EnableAspectJAutoProxy
@EnableJpaRepositories(basePackages = {
        "org.example.walletservice.repository",
        "org.example.shared.repository"
})
@EntityScan(basePackages = {
        "org.example.walletservice.entity",
        "org.example.shared.entity"
})
public class WalletServiceApplication {

    public static void main(String[] args) {
        SpringApplication.run(WalletServiceApplication.class, args);
    }

}
