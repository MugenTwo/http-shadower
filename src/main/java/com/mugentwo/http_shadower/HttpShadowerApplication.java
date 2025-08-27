package com.mugentwo.http_shadower;

import com.mugentwo.http_shadower.config.ShadowerConfiguration;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;

@SpringBootApplication
@EnableConfigurationProperties(ShadowerConfiguration.class)
public class HttpShadowerApplication {

	public static void main(String[] args) {
		SpringApplication.run(HttpShadowerApplication.class, args);
	}

}
