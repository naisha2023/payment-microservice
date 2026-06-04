package org.example.shared.config;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.env.EnvironmentPostProcessor;
import org.springframework.core.Ordered;
import org.springframework.core.env.ConfigurableEnvironment;
import org.springframework.core.env.MapPropertySource;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

public class DockerSecretsEnvironmentPostProcessor
        implements EnvironmentPostProcessor, Ordered {

    private static final String FILE_SUFFIX = "_FILE";

    @Override
    public void postProcessEnvironment(
            ConfigurableEnvironment environment,
            SpringApplication application) {

        System.out.println(">>> DOCKER SECRETS LOADED <<<");

        Map<String, Object> secrets = new HashMap<>();

        System.getenv().forEach((key, value) -> {

            if (!key.endsWith(FILE_SUFFIX)) {
                return;
            }

            String envKey = key.substring(0, key.length() - FILE_SUFFIX.length());
            String propertyKey = toPropertyKey(envKey);

            try {
                Path secretPath = Path.of(value);
                
                System.out.println("secretPath=" + secretPath);
                System.out.println(Files.exists(secretPath));

                if (Files.exists(secretPath)) {

                    String secretValue =
                            Files.readString(secretPath).trim();
                    
                    System.out.println("ssl.password length=" + secretValue.length());
                    System.out.println("ssl.password first=" + secretValue.charAt(0));
                    System.out.println("ssl.password last=" + secretValue.charAt(secretValue.length() - 1));

                    System.out.println("Resolved ssl.password=[" +
                        environment.getProperty("ssl.password") + "]");

                    System.out.println(
                            "Loaded secret: " + propertyKey
                    );

                    secrets.put(propertyKey, secretValue); 
                    secrets.put(envKey, secretValue);
                }

            } catch (Exception e) {
                throw new IllegalStateException(
                        "Unable to read Docker secret from "
                                + value
                                + " for "
                                + key,
                        e
                );
            }
        });

        if (!secrets.isEmpty()) {
            environment.getPropertySources().addFirst(
                    new MapPropertySource(
                            "dockerSecrets",
                            secrets
                    )
            );
        }
    }

    private String toPropertyKey(String envKey) {
        return envKey.toLowerCase().replace('_', '.');
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}