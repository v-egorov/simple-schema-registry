package ru.vegorov.schemaregistry;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

@SpringBootApplication(scanBasePackages = "ru.vegorov.schemaregistry")
public class SchemaRegistryApplication {

    public static void main(String[] args) {
        SpringApplication.run(SchemaRegistryApplication.class, args);
    }
}