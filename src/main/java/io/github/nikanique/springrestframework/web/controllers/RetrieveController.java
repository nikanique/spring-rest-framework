package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.swagger.RetrieveSchemaGenerator;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.method.HandlerMethod;

import java.util.List;
import java.util.Optional;

@Getter
public abstract class RetrieveController<EntityClass, ID, ModelRepository extends JpaRepository<EntityClass, ID> & JpaSpecificationExecutor<EntityClass>>
        extends BaseGenericController<EntityClass, ID, ModelRepository>
        implements RetrieveSchemaGenerator {

    final private SerializerConfig retrieveSerializerConfig;
    final private Filter lookupFilter;
    private QueryService<EntityClass> queryService;

    public RetrieveController(ModelRepository repository) {
        super(repository);
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

    public SerializerConfig configRetrieveSerializer() {
        return SerializerConfig.fromDTO(getRetrieveResponseDTO());
    }

    protected Filter configLookupFilter() {
        return new Filter("id", FilterOperation.EQUAL, FieldType.INTEGER);
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
        if (handlerMethod.getMethod().getName().equals("getByLookupValue")) {
            this.generateRetrieveSchema(operation, this.getLookupFilter(), this.getRetrieveResponseDTO());
        }
    }


}
