package com.eshop.cart;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.eshop.cart", "com.eshop.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class EshopCartApplication {
    public static void main(String[] args) {
        SpringApplication.run(EshopCartApplication.class, args);
    }
}
