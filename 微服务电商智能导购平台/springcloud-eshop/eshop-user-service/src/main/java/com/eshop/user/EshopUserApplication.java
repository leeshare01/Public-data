package com.eshop.user;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.eshop.user", "com.eshop.common"})
@EnableDiscoveryClient
public class EshopUserApplication {
    public static void main(String[] args) {
        SpringApplication.run(EshopUserApplication.class, args);
    }
}
