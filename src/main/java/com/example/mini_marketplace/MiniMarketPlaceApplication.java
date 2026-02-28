package com.example.mini_marketplace;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class MiniMarketPlaceApplication {

    public static void main(String[] args) {
        SpringApplication.run(MiniMarketPlaceApplication.class, args);
    }

}


