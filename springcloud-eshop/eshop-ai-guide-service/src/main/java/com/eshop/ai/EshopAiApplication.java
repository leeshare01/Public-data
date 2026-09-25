package com.eshop.ai;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.eshop.ai", "com.eshop.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class EshopAiApplication {
    public static void main(String[] args) {
        SpringApplication.run(EshopAiApplication.class, args);
    }
}
