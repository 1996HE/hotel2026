package com.example.minshuku;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * 民宿管理システムの Spring Boot 起動クラス。
 * <p>
 * アプリケーション全体の起点であり、ここ自体に業務ロジックは持たせない。
 */
@SpringBootApplication
public class MinshukuManagementApplication {
    public static void main(String[] args) {
        SpringApplication.run(MinshukuManagementApplication.class, args);
    }
}
