package com.eshop.order;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;
import org.springframework.cloud.openfeign.EnableFeignClients;

@SpringBootApplication(scanBasePackages = {"com.eshop.order", "com.eshop.common"})
@EnableDiscoveryClient
@EnableFeignClients
public class EshopOrderApplication {
    public static void main(String[] args) {
        SpringApplication.run(EshopOrderApplication.class, args);
    }
}
