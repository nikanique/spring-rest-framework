package io.github.nikanique.springrestframework.web.controllers;


import io.github.nikanique.springrestframework.exceptions.UnauthorizedException;
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
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.ParameterizedType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This is abstract controller. It is a base controller for all generic controllers.
 *
 * @param <Model>           The model
 * @param <ID>              Type of model's primary key (id)
 * @param <ModelRepository> Type of model's repository
 */
public abstract class BaseGenericController<Model, ID, ModelRepository extends JpaRepository<Model, ID>>
        implements ApplicationContextAware {

    final protected ModelRepository repository;
    private final Map<String, List<String>> endpointsRequiredAuthorities;
    @Getter
    protected Serializer serializer;
    protected ApplicationContext context;

    @Autowired
    public BaseGenericController(ModelRepository repository) {
        this.repository = repository;
        this.endpointsRequiredAuthorities = new HashMap<>();
        this.configRequiredAuthorities(this.endpointsRequiredAuthorities);
    }


    @Override
    @Autowired
    public void setApplicationContext(ApplicationContext applicationContext) throws BeansException {
        this.context = applicationContext;
    }


    public abstract void customizeOperationForController(Operation operation, HandlerMethod handlerMethod);

    /**
     * This method must be overridden to return the class type of the DTO (Data Transfer Object)
     * that the controller will use to serialize/deserialize the model’s records/request's body.
     * Note that the DTO class must be a subclass of Dto class.
     * <pre>
     * {@code
     * @Override
     * protected Class<?> getDTO() {
     *     return StudentDto.class;
     * }
     * }
     * </pre>
     *
     * @return The class type of the DTO
     */
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

    protected void configRequiredAuthorities(Map<String, List<String>> authorities) {
        authorities.put("GET", null);
        authorities.put("POST", null);
        authorities.put("PUT", null);
        authorities.put("PATCH", null);
        authorities.put("DELETE", null);
    }

    protected List<String> getRequiredAuthorities(String HttpMethod) {
        return this.endpointsRequiredAuthorities.getOrDefault(HttpMethod, null);
    }

    protected boolean hasAuthorities(List<String> requiredAuthorities) {
        if (requiredAuthorities == null) {
            return true;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        return requiredAuthorities.stream().anyMatch(authority ->
                authentication.getAuthorities().stream().anyMatch(a -> a.getAuthority().equals(authority))
        );
    }

    protected void authorizeRequest(HttpServletRequest request) {
        List<String> requiredAuthorities = getRequiredAuthorities(request.getMethod().toUpperCase());
        if (!hasAuthorities(requiredAuthorities)) {
            throw new UnauthorizedException("You do not have permission to perform this action.");
        }
    }
}
