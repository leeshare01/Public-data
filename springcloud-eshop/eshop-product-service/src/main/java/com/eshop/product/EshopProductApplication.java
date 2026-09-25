package com.eshop.product;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication(scanBasePackages = {"com.eshop.product", "com.eshop.common"})
@EnableDiscoveryClient
public class EshopProductApplication {
    public static void main(String[] args) {
        SpringApplication.run(EshopProductApplication.class, args);
    }
}
