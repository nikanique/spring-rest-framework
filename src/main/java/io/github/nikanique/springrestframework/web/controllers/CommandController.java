package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.orm.EntityBuilder;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.CommandService;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.swagger.DeleteSchemaGenerator;
import io.github.nikanique.springrestframework.swagger.UpdateSchemaGenerator;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Getter
public abstract class CommandController<Model, ID, ModelRepository extends JpaRepository<Model, ID> & JpaSpecificationExecutor<Model>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements ICreateController<Model, ID>, UpdateSchemaGenerator, DeleteSchemaGenerator {


    final private Filter lookupFilter;
    final private SerializerConfig updateResponseSerializerConfig;
    final private SerializerConfig createResponseSerializerConfig;
    private EntityBuilder<Model> entityHelper;
    private CommandService<Model, ID> commandService;
    private QueryService<Model> queryService;


    @Autowired
    public CommandController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ModelRepository repository) {
        super(repository);

        this.updateResponseSerializerConfig = configUpdateResponseSerializerFields();
        this.createResponseSerializerConfig = configCreateSerializerFields();
        this.lookupFilter = configLookupFilter();
    }


    @PostConstruct
    private void postConstruct() {
        this.commandService = CommandService.getInstance(this.getModel(), this.repository, this.context);
        this.queryService = QueryService.getInstance(this.getModel(), this.repository, this.context);
        this.entityHelper = EntityBuilder.getInstance(this.getModel(), this.context);
    }

    public Class<?> getCreateRequestBodyDTO() {
        return getDTO();
    }

    public Class<?> getCreateResponseBodyDTO() {
        return getDTO();
    }

    protected Class<?> getUpdateRequestBodyDTO() {
        return getDTO();
    }

    protected Class<?> getUpdateResponseBodyDTO() {
        return getDTO();
    }

    public SerializerConfig configCreateSerializerFields() {
        return SerializerConfig.fromDTO(getCreateResponseBodyDTO());
    }

    public SerializerConfig configUpdateResponseSerializerFields() {
        return SerializerConfig.fromDTO(getUpdateResponseBodyDTO());
    }

    protected Filter configLookupFilter() {
        return new Filter("id", FilterOperation.EQUAL, FieldType.INTEGER);
    }

    @PostMapping("/")
    public ResponseEntity<ObjectNode> post(HttpServletRequest request) throws IOException {
        return this.create(this, request);
    }

    @PutMapping("/{lookup}")
    public ResponseEntity<ObjectNode> update(@PathVariable(name = "lookup") Object lookupValue, HttpServletRequest request) throws Throwable {
        // Create search criteria from lookup value
        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromValue(lookupValue, this.getLookupFilter());
        searchCriteriaList = this.filterByRequest(request, searchCriteriaList);

        // Retrieve the entity using specification
        Optional<Object> optionalEntity = this.queryService.getObject(searchCriteriaList);
        if (!optionalEntity.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        String requestBody = getRequestBody(request);
        Object dto = serializer.deserialize(requestBody, this.getUpdateRequestBodyDTO(), true);

        // Partially update the entity fields except the lookup field
        Model entityFromDB = commandService.update((Model) optionalEntity.get(), dto, this.getLookupFilter().getName(), this.getUpdateRequestBodyDTO());

        // Return the updated entity
        return ResponseEntity.status(HttpStatus.OK).body(
                serializer.serialize(entityFromDB, getUpdateResponseSerializerConfig())
        );
    }

    @PatchMapping("/{lookup}")
    public ResponseEntity<ObjectNode> partialUpdate(@PathVariable(name = "lookup") Object lookupValue, HttpServletRequest request) throws Throwable {
        // Create search criteria from lookup value
        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromValue(lookupValue, this.getLookupFilter());
        searchCriteriaList = this.filterByRequest(request, searchCriteriaList);

        // Retrieve the entity using specification
        Optional<Object> optionalEntity = this.queryService.getObject(searchCriteriaList);
        if (!optionalEntity.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        String requestBody = getRequestBody(request);
        Set<String> presentFields = serializer.getPresentFields(requestBody);
        Object dto = serializer.deserialize(requestBody, this.getUpdateRequestBodyDTO(), true, presentFields);

        // Partially update the entity fields except the lookup field
        Model entityFromDB = commandService.update((Model) optionalEntity.get(), dto, this.getLookupFilter().getName(), this.getUpdateRequestBodyDTO(), presentFields);

        // Return the updated entity
        return ResponseEntity.status(HttpStatus.OK).body(
                serializer.serialize(entityFromDB, getUpdateResponseSerializerConfig())
        );
    }


    @DeleteMapping("/{lookup}")
    public ResponseEntity<Void> delete(HttpServletRequest request, @PathVariable(name = "lookup") Object lookupValue) {
        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromValue(lookupValue, this.getLookupFilter());
        searchCriteriaList = this.filterByRequest(request, searchCriteriaList);

        // Retrieve the entity using specification
        Optional<Object> optionalEntity = this.queryService.getObject(searchCriteriaList);

        if (!optionalEntity.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        commandService.delete((Model) optionalEntity.get());
        return ResponseEntity.noContent().build();
    }


    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("post")) {
            generateCreateSchema(operation, getCreateRequestBodyDTO(), getCreateResponseBodyDTO());
        } else if (handlerMethod.getMethod().getName().equals("update") || handlerMethod.getMethod().getName().equals("partialUpdate")) {
            generateUpdateSchema(operation, getLookupFilter(), getUpdateRequestBodyDTO(), getUpdateResponseBodyDTO());
        } else if (handlerMethod.getMethod().getName().equals("delete")) {
            generateDeleteSchema(operation, getLookupFilter());
        }
    }


}
