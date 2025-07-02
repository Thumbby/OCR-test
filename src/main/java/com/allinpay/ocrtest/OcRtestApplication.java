package com.allinpay.ocrtest;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.web.servlet.ServletComponentScan;

@SpringBootApplication(scanBasePackages = "com.allinpay.ocrtest")
@ServletComponentScan
public class OcRtestApplication {

    public static void main(String[] args) {
        SpringApplication.run(OcRtestApplication.class, args);
    }

}
