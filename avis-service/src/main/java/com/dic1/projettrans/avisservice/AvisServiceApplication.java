package com.dic1.projettrans.avisservice;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.cloud.openfeign.EnableFeignClients;


@SpringBootApplication
@EnableFeignClients(basePackages = "com.dic1.projettrans.avisservice.feign")
public class AvisServiceApplication {
    public static void main(String[] args) {
        SpringApplication.run(AvisServiceApplication.class, args);
    }
}