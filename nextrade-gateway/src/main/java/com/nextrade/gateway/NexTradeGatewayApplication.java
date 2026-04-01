package com.nextrade.gateway;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.client.discovery.EnableDiscoveryClient;

@SpringBootApplication
@EnableDiscoveryClient
public class NexTradeGatewayApplication {
    public static void main(String[] args) {
        SpringApplication.run(NexTradeGatewayApplication.class, args);
    }
}
