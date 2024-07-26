package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.exceptions.ValidationException;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.filter.FilterSet;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.swagger.ListSchemaGenerator;
import io.github.nikanique.springrestframework.swagger.RetrieveSchemaGenerator;
import io.github.nikanique.springrestframework.web.responses.PagedResponse;
import io.swagger.v3.oas.models.Operation;
import lombok.Getter;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import javax.annotation.PostConstruct;
import javax.servlet.http.HttpServletRequest;
import java.util.List;
import java.util.Optional;
import java.util.TreeSet;

@Getter
public abstract class QueryController<EntityClass, ID, ModelRepository extends JpaRepository<EntityClass, ID> & JpaSpecificationExecutor<EntityClass>>
        extends BaseGenericController<EntityClass, ID, ModelRepository>
        implements ListSchemaGenerator, RetrieveSchemaGenerator {

    final private SerializerConfig listSerializerConfig;
    final private SerializerConfig retrieveSerializerConfig;
    final private Filter lookupFilter;
    final private FilterSet filterSet;
    private QueryService<EntityClass> queryService;

    public QueryController(ModelRepository repository) {
        super(repository);
        this.filterSet = configFilterSet();
        this.listSerializerConfig = configListSerializer();
        this.retrieveSerializerConfig = configRetrieveSerializer();
        this.lookupFilter = configLookupFilter();
    }


    @PostConstruct
    private void postConstruct() {
        this.queryService = QueryService.getInstance(this.getEntityClass(), this.repository, this.context);
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

    public SerializerConfig configRetrieveSerializer() {
        return SerializerConfig.fromDTO(getRetrieveResponseDTO());
    }

    protected Filter configLookupFilter() {
        return new Filter("id", FilterOperation.EQUAL, FieldType.INTEGER);
    }

    @GetMapping("/")
    public ResponseEntity<PagedResponse<ObjectNode>> listAll(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "id") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) throws ValidationException {

        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromUrlQuery(request, filterSet);
        searchCriteriaList = this.filterByRequest(request, searchCriteriaList);

        Page<EntityClass> entityPage = queryService.list(searchCriteriaList, page, size, direction, sortBy);
        List<ObjectNode> dtoList = entityPage.map(entity -> serializer.serialize(entity, getListSerializerConfig())).getContent();
        PagedResponse<ObjectNode> response = new PagedResponse<>(dtoList, entityPage.getTotalElements());
        return ResponseEntity.ok(response);
    }

    @GetMapping("/{lookup}")
    public ResponseEntity<ObjectNode> getByLookupValue(
            HttpServletRequest request,
            @PathVariable(name = "lookup") Object lookupValue) {

        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromValue(lookupValue, this.getLookupFilter());
        searchCriteriaList = this.filterByRequest(request, searchCriteriaList);

        Optional<EntityClass> optionalEntity = queryService.get(searchCriteriaList);
        return optionalEntity.map(entity -> ResponseEntity.ok(
                        serializer.serialize(entity, getRetrieveSerializerConfig())
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("listAll")) {
            this.generateListSchema(operation, this.getFilterSet().getFilters(), this.getListResponseDTO());
        } else if (handlerMethod.getMethod().getName().equals("getByLookupValue")) {
            this.generateRetrieveSchema(operation, this.getLookupFilter(), this.getRetrieveResponseDTO());
        }
    }


}