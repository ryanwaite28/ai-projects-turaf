package com.turaf.experiment.infrastructure.config;

import com.turaf.common.tenant.TenantInterceptor;
import org.hibernate.cfg.AvailableSettings;
import org.springframework.boot.autoconfigure.orm.jpa.HibernatePropertiesCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * JPA/Hibernate configuration for experiment service.
 * Configures the TenantInterceptor for automatic organizationId management.
 */
@Configuration
public class JpaConfig {
    
    /**
     * Configure Hibernate to use TenantInterceptor.
     * This enables automatic setting of organizationId on TenantAware entities.
     *
     * @return HibernatePropertiesCustomizer that adds the interceptor
     */
    @Bean
    public HibernatePropertiesCustomizer hibernatePropertiesCustomizer() {
        return hibernateProperties -> 
            hibernateProperties.put(AvailableSettings.INTERCEPTOR, new TenantInterceptor());
    }
}
