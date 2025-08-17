package com.ecommerce.shared.security.repository;

import com.ecommerce.shared.models.TenantAware;
import com.ecommerce.shared.utils.TenantContext;
import com.ecommerce.shared.utils.exception.TenantAccessDeniedException;
import org.springframework.data.domain.Example;
import org.springframework.data.jpa.repository.support.JpaEntityInformation;
import org.springframework.data.jpa.repository.support.SimpleJpaRepository;
import org.springframework.util.StringUtils;

import jakarta.persistence.EntityManager;
import jakarta.persistence.TypedQuery;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.util.List;
import java.util.Optional;

/**
 * Base repository implementation with automatic tenant filtering
 */
public class BaseTenantAwareRepository<T extends TenantAware, ID> 
        extends SimpleJpaRepository<T, ID> implements TenantAwareRepository<T, ID> {

    private final EntityManager entityManager;
    private final JpaEntityInformation<T, ?> entityInformation;

    public BaseTenantAwareRepository(JpaEntityInformation<T, ?> entityInformation, 
                                   EntityManager entityManager) {
        super(entityInformation, entityManager);
        this.entityManager = entityManager;
        this.entityInformation = entityInformation;
    }

    @Override
    public Optional<T> findById(ID id) {
        String tenantId = getCurrentTenantId();
        return findByIdAndTenantId(id, tenantId);
    }

    @Override
    public List<T> findAll() {
        String tenantId = getCurrentTenantId();
        return findByTenantId(tenantId);
    }

    @Override
    public void deleteById(ID id) {
        String tenantId = getCurrentTenantId();
        deleteByIdAndTenantId(id, tenantId);
    }

    @Override
    public boolean existsById(ID id) {
        String tenantId = getCurrentTenantId();
        return existsByIdAndTenantId(id, tenantId);
    }

    @Override
    public long count() {
        String tenantId = getCurrentTenantId();
        return countByTenantId(tenantId);
    }

    @Override
    public Optional<T> findByIdAndTenantId(ID id, String tenantId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(getDomainClass());
        Root<T> root = query.from(getDomainClass());
        
        Predicate idPredicate = cb.equal(root.get(entityInformation.getIdAttribute()), id);
        Predicate tenantPredicate = cb.equal(root.get("tenantId"), tenantId);
        
        query.select(root).where(cb.and(idPredicate, tenantPredicate));
        
        TypedQuery<T> typedQuery = entityManager.createQuery(query);
        List<T> results = typedQuery.getResultList();
        
        return results.isEmpty() ? Optional.empty() : Optional.of(results.get(0));
    }

    @Override
    public List<T> findByTenantId(String tenantId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<T> query = cb.createQuery(getDomainClass());
        Root<T> root = query.from(getDomainClass());
        
        query.select(root).where(cb.equal(root.get("tenantId"), tenantId));
        
        return entityManager.createQuery(query).getResultList();
    }

    @Override
    public void deleteByIdAndTenantId(ID id, String tenantId) {
        Optional<T> entity = findByIdAndTenantId(id, tenantId);
        entity.ifPresent(this::delete);
    }

    @Override
    public boolean existsByIdAndTenantId(ID id, String tenantId) {
        return findByIdAndTenantId(id, tenantId).isPresent();
    }

    @Override
    public long countByTenantId(String tenantId) {
        CriteriaBuilder cb = entityManager.getCriteriaBuilder();
        CriteriaQuery<Long> query = cb.createQuery(Long.class);
        Root<T> root = query.from(getDomainClass());
        
        query.select(cb.count(root)).where(cb.equal(root.get("tenantId"), tenantId));
        
        return entityManager.createQuery(query).getSingleResult();
    }

    @Override
    public <S extends T> S save(S entity) {
        String tenantId = getCurrentTenantId();
        
        // Set tenant ID if not already set
        if (!StringUtils.hasText(entity.getTenantId())) {
            entity.setTenantId(tenantId);
        } else if (!tenantId.equals(entity.getTenantId())) {
            // Prevent cross-tenant data access
            throw new TenantAccessDeniedException("Cannot save entity for different tenant");
        }
        
        return super.save(entity);
    }

    @Override
    public <S extends T> List<S> saveAll(Iterable<S> entities) {
        String tenantId = getCurrentTenantId();
        
        // Validate and set tenant ID for all entities
        for (S entity : entities) {
            if (!StringUtils.hasText(entity.getTenantId())) {
                entity.setTenantId(tenantId);
            } else if (!tenantId.equals(entity.getTenantId())) {
                throw new TenantAccessDeniedException("Cannot save entity for different tenant");
            }
        }
        
        return super.saveAll(entities);
    }

    private String getCurrentTenantId() {
        String tenantId = TenantContext.getTenantId();
        if (!StringUtils.hasText(tenantId)) {
            throw new TenantAccessDeniedException("No tenant context available");
        }
        return tenantId;
    }
}