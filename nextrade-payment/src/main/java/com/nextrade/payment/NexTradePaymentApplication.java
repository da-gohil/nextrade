package com.nextrade.payment;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NexTradePaymentApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexTradePaymentApplication.class, args);
    }
}
