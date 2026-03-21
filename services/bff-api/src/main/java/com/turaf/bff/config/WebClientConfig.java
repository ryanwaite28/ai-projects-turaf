package com.turaf.bff.config;

import io.netty.channel.ChannelOption;
import io.netty.handler.timeout.ReadTimeoutHandler;
import io.netty.handler.timeout.WriteTimeoutHandler;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.reactive.ReactorClientHttpConnector;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.netty.http.client.HttpClient;

import java.time.Duration;
import java.util.concurrent.TimeUnit;

@Slf4j
@Configuration
@RequiredArgsConstructor
public class WebClientConfig {
    
    private final ServiceUrlsConfig serviceUrlsConfig;
    
    @Bean(name = "identityWebClient")
    public WebClient identityWebClient() {
        String baseUrl = serviceUrlsConfig.getIdentityUrl();
        log.info("Configuring Identity Service WebClient with base URL: {}", baseUrl);
        return createWebClient(baseUrl);
    }
    
    @Bean(name = "organizationWebClient")
    public WebClient organizationWebClient() {
        String baseUrl = serviceUrlsConfig.getOrganizationUrl();
        log.info("Configuring Organization Service WebClient with base URL: {}", baseUrl);
        return createWebClient(baseUrl);
    }
    
    @Bean(name = "experimentWebClient")
    public WebClient experimentWebClient() {
        String baseUrl = serviceUrlsConfig.getExperimentUrl();
        log.info("Configuring Experiment Service WebClient with base URL: {}", baseUrl);
        return createWebClient(baseUrl);
    }
    
    @Bean(name = "metricsWebClient")
    public WebClient metricsWebClient() {
        String baseUrl = serviceUrlsConfig.getMetricsUrl();
        log.info("Configuring Metrics Service WebClient with base URL: {}", baseUrl);
        return createWebClient(baseUrl);
    }
    
    private WebClient createWebClient(String baseUrl) {
        HttpClient httpClient = HttpClient.create()
            .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 5000)
            .responseTimeout(Duration.ofSeconds(10))
            .doOnConnected(conn -> 
                conn.addHandlerLast(new ReadTimeoutHandler(10, TimeUnit.SECONDS))
                    .addHandlerLast(new WriteTimeoutHandler(10, TimeUnit.SECONDS)));
        
        return WebClient.builder()
            .baseUrl(baseUrl)
            .clientConnector(new ReactorClientHttpConnector(httpClient))
            .build();
    }
}
