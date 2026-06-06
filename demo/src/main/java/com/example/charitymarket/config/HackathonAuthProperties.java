package com.example.charitymarket.config;

import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Getter
@Setter
@Component
@ConfigurationProperties(prefix = "app.auth")
public class HackathonAuthProperties {

    private AuthMode mode = AuthMode.DEMO;
    private String aliceEmail = "alice@example.com";
    private String bobEmail = "bob@example.com";
    private String aliceToken;
    private String bobToken;
}
