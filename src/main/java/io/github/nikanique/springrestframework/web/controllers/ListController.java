package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.exceptions.ValidationException;
import io.github.nikanique.springrestframework.filter.FilterSet;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.swagger.ListSchemaGenerator;
import io.github.nikanique.springrestframework.utilities.MethodReflectionHelper;
import io.github.nikanique.springrestframework.web.responses.PagedResponse;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.List;
import java.util.TreeSet;

@Getter
public abstract class ListController<Model, ID, ModelRepository extends JpaRepository<Model, ID> & JpaSpecificationExecutor<Model>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements ListSchemaGenerator {

    final private SerializerConfig listSerializerConfig;
    final private FilterSet filterSet;
    final private Method queryMethod;
    private QueryService<Model> queryService;

    public ListController(ModelRepository repository) throws NoSuchMethodException {
        super(repository);
        this.filterSet = configFilterSet();
        this.listSerializerConfig = configListSerializer();
        this.queryMethod = MethodReflectionHelper.findRepositoryMethod(getQueryMethodName(), repository);

    }

    protected String getQueryMethodName() {
        return "findAll";
    }

    @PostConstruct
    private void postConstruct() {
        this.queryService = QueryService.getInstance(this.getModel(), this.repository, this.context);
    }


    protected Class<?> getListResponseDTO() {
        return getDTO();
    }

    protected Class<?> getRetrieveResponseDTO() {
        return getDTO();
    }

    protected FilterSet configFilterSet() {
        return new FilterSet(new TreeSet<>());
    }

    public SerializerConfig configListSerializer() {
        return SerializerConfig.fromDTO(getListResponseDTO());
    }

    @GetMapping("/")
    public ResponseEntity<PagedResponse<ObjectNode>> listAll(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) throws ValidationException, InvocationTargetException, IllegalAccessException {

        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromUrlQuery(request, filterSet);
        searchCriteriaList = this.filterByRequest(request, searchCriteriaList);

        Page<Object> entityPage = queryService.list(searchCriteriaList, page, size, direction, sortBy, getQueryMethod());
        List<ObjectNode> dtoList = entityPage.map(entity -> serializer.serialize(entity, getListSerializerConfig())).getContent();
        PagedResponse<ObjectNode> response = new PagedResponse<>(dtoList, entityPage.getTotalElements());
        return ResponseEntity.ok(response);
    }


    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("listAll")) {
            this.generateListSchema(operation, this.getFilterSet().getFilters(), this.getListResponseDTO());
        }
    }


}
