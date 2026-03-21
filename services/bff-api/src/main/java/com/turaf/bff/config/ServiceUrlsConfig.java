package com.turaf.bff.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Data
@Configuration
@ConfigurationProperties(prefix = "services")
public class ServiceUrlsConfig {
    
    private ServiceUrl identity = new ServiceUrl();
    private ServiceUrl organization = new ServiceUrl();
    private ServiceUrl experiment = new ServiceUrl();
    private ServiceUrl metrics = new ServiceUrl();
    
    @Data
    public static class ServiceUrl {
        private String url;
    }
    
    public String getIdentityUrl() {
        return identity.getUrl();
    }
    
    public String getOrganizationUrl() {
        return organization.getUrl();
    }
    
    public String getExperimentUrl() {
        return experiment.getUrl();
    }
    
    public String getMetricsUrl() {
        return metrics.getUrl();
    }
}
