package com.smartfitness;

import org.mybatis.spring.annotation.MapperScan;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;


@MapperScan("com.smartfitness.mapper")
@SpringBootApplication
public class SmartFitnessApplication {

    public static void main(String[] args) {
        SpringApplication.run(SmartFitnessApplication.class, args);
    }

}
