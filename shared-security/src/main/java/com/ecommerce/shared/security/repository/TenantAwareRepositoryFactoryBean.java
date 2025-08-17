package com.ecommerce.shared.security.repository;

import com.ecommerce.shared.models.TenantAware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactory;
import org.springframework.data.jpa.repository.support.JpaRepositoryFactoryBean;
import org.springframework.data.jpa.repository.support.JpaRepositoryImplementation;
import org.springframework.data.repository.core.RepositoryInformation;
import org.springframework.data.repository.core.RepositoryMetadata;
import org.springframework.data.repository.core.support.RepositoryFactorySupport;

import jakarta.persistence.EntityManager;
import java.io.Serializable;

/**
 * Factory bean for creating tenant-aware repositories
 */
public class TenantAwareRepositoryFactoryBean<R extends JpaRepository<T, I>, T, I extends Serializable>
        extends JpaRepositoryFactoryBean<R, T, I> {

    public TenantAwareRepositoryFactoryBean(Class<? extends R> repositoryInterface) {
        super(repositoryInterface);
    }

    @Override
    protected RepositoryFactorySupport createRepositoryFactory(EntityManager entityManager) {
        return new TenantAwareRepositoryFactory(entityManager);
    }

    private static class TenantAwareRepositoryFactory extends JpaRepositoryFactory {

        public TenantAwareRepositoryFactory(EntityManager entityManager) {
            super(entityManager);
        }

        @Override
        protected JpaRepositoryImplementation<?, ?> getTargetRepository(
                RepositoryInformation information, EntityManager entityManager) {

            JpaEntityInformation<?, Serializable> entityInformation = 
                    getEntityInformation(information.getDomainType());

            Object repository = getTargetRepositoryViaReflection(
                    information, entityInformation, entityManager);

            if (repository instanceof BaseTenantAwareRepository) {
                return (JpaRepositoryImplementation<?, ?>) repository;
            }

            return super.getTargetRepository(information, entityManager);
        }

        @Override
        protected Class<?> getRepositoryBaseClass(RepositoryMetadata metadata) {
            Class<?> domainType = metadata.getDomainType();
            
            if (TenantAware.class.isAssignableFrom(domainType)) {
                return BaseTenantAwareRepository.class;
            }
            
            return super.getRepositoryBaseClass(metadata);
        }
    }
}