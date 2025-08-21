package com.ecommerce.userservice.config;

import com.ecommerce.shared.security.TenantAwareEntityListener;
import com.ecommerce.shared.security.repository.TenantAwareRepositoryFactoryBean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;

import jakarta.persistence.EntityListeners;

/**
 * JPA configuration for user service with tenant-aware repositories
 */
@Configuration
@EnableJpaRepositories(
    basePackages = "com.ecommerce.userservice.repository",
    repositoryFactoryBeanClass = TenantAwareRepositoryFactoryBean.class
)
@EntityListeners(TenantAwareEntityListener.class)
public class JpaConfig {
}