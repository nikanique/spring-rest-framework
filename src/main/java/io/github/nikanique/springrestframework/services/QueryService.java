package io.github.nikanique.springrestframework.services;


import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.orm.SpecificationsBuilder;
import org.springframework.context.ApplicationContext;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.ConcurrentHashMap;

@SuppressWarnings({"unchecked", "rawtypes"})
public class QueryService<Model> {

    private static final ConcurrentHashMap<Class<?>, QueryService<?>> instances = new ConcurrentHashMap<>();

    private final JpaSpecificationExecutor<Model> jpaSpecificationExecutor;

    private final SpecificationsBuilder specificationsBuilder;

    private QueryService(JpaSpecificationExecutor<Model> jpaSpecificationExecutor, ApplicationContext springContext) {
        this.jpaSpecificationExecutor = jpaSpecificationExecutor;
        this.specificationsBuilder = springContext.getBean(SpecificationsBuilder.class);
    }

    public static <Model> QueryService<Model> getInstance(
            Class<Model> entityClass,
            JpaSpecificationExecutor<Model> jpaSpecificationExecutor,
            ApplicationContext springContext) {

        return (QueryService<Model>) instances.computeIfAbsent(entityClass, k -> new QueryService<>(jpaSpecificationExecutor, springContext));
    }

    public Optional<Model> get(List<SearchCriteria> searchCriteriaList) {
        Specification specifications = this.specificationsBuilder.fromSearchCriteriaList(searchCriteriaList);
        return jpaSpecificationExecutor.findOne(specifications);
    }

    public Page<Model> list(List<SearchCriteria> searchCriteriaList, int page, int size, Sort.Direction direction, String sortBy) {
        Specification specifications = this.specificationsBuilder.fromSearchCriteriaList(searchCriteriaList);
        Pageable pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        return jpaSpecificationExecutor.findAll(specifications, pageable);
    }
}
