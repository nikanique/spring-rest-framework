package io.github.nikanique.springrestframework.web.controllers;


import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.Serializer;
import io.swagger.v3.oas.models.Operation;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ApplicationContextAware;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.ParameterizedType;
import java.util.List;


public abstract class BaseGenericController<Model, ID, ModelRepository extends JpaRepository<Model, ID>>
        implements ApplicationContextAware {

    final protected ModelRepository repository;

    @Getter
    protected Serializer serializer;
    protected ApplicationContext context;

    @Autowired
    public BaseGenericController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ModelRepository repository) {
        this.repository = repository;
    }

    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }


    public abstract void customizeOperationForController(Operation operation, HandlerMethod handlerMethod);

    protected abstract Class<?> getDTO();

    protected Class<Model> getModel() {
        @SuppressWarnings("unchecked")
        Class<Model> entityClass = (Class<Model>) ((ParameterizedType) getClass().getGenericSuperclass()).getActualTypeArguments()[0];
        return entityClass;
    }

    protected List<SearchCriteria> filterByRequest(HttpServletRequest request, List<SearchCriteria> searchCriteria) {
        return searchCriteria;
    }

    @Autowired
    public void setSerializer(Serializer serializer) {
        this.serializer = serializer;
    }
}
