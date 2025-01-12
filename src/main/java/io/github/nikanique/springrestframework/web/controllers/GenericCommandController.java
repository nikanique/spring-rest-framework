package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.orm.EntityBuilder;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.CommandService;
import io.github.nikanique.springrestframework.services.QueryService;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.method.HandlerMethod;

@Getter
public abstract class GenericCommandController<Model, ID, ModelRepository extends JpaRepository<Model, ID> & JpaSpecificationExecutor<Model>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements CreateController<Model, ID>, UpdateController<Model, ID>, DeleteController<Model, ID> {


    final private Filter lookupFilter;
    final private SerializerConfig updateResponseSerializerConfig;
    final private SerializerConfig createResponseSerializerConfig;
    private EntityBuilder<Model> entityHelper;
    private CommandService<Model, ID> commandService;
    private QueryService<Model> queryService;


    @Autowired
    public GenericCommandController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ModelRepository repository) {
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

    public Class<?> getUpdateRequestBodyDTO() {
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
    public ResponseEntity<ObjectNode> post(HttpServletRequest request) throws Throwable {
        this.authorizeRequest(request);
        return this.create(this, request);
    }

    @PutMapping("/{lookup}")
    public ResponseEntity<ObjectNode> put(@PathVariable(name = "lookup") Object lookupValue, HttpServletRequest request) throws Throwable {
        this.authorizeRequest(request);
        return this.update(this, lookupValue, request);
    }


    @PatchMapping("/{lookup}")
    public ResponseEntity<ObjectNode> patch(@PathVariable(name = "lookup") Object lookupValue, HttpServletRequest request) throws Throwable {
        this.authorizeRequest(request);
        return this.partialUpdate(this, lookupValue, request);
    }


    @DeleteMapping("/{lookup}")
    public ResponseEntity<Void> delete(HttpServletRequest request, @PathVariable(name = "lookup") Object lookupValue) {
        this.authorizeRequest(request);
        return this.deleteObject(this, lookupValue, request);
    }


    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("post")) {
            generateCreateSchema(operation, getCreateRequestBodyDTO(), getCreateResponseBodyDTO());
        } else if (handlerMethod.getMethod().getName().equals("put") || handlerMethod.getMethod().getName().equals("patch")) {
            generateUpdateSchema(operation, getLookupFilter(), getUpdateRequestBodyDTO(), getUpdateResponseBodyDTO());
        } else if (handlerMethod.getMethod().getName().equals("delete")) {
            generateDeleteSchema(operation, getLookupFilter());
        }
    }


}
