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
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.method.HandlerMethod;

@Getter
public abstract class GenericUpdateController<Model, ID, ModelRepository extends JpaRepository<Model, ID> & JpaSpecificationExecutor<Model>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements UpdateController {


    final private Filter lookupFilter;
    final private SerializerConfig updateResponseSerializerConfig;
    private EntityBuilder<Model> entityHelper;
    private CommandService<Model, ID> commandService;
    private QueryService<Model> queryService;


    @Autowired
    public GenericUpdateController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ModelRepository repository) throws NoSuchMethodException {
        super(repository);
        this.updateResponseSerializerConfig = configUpdateResponseSerializerFields();
        this.lookupFilter = configLookupFilter();

    }


    @PostConstruct
    private void postConstruct() {
        this.commandService = CommandService.getInstance(this.getModel(), this.repository, this.context);
        this.queryService = QueryService.getInstance(this.getModel(), this.repository, this.context);
        this.entityHelper = EntityBuilder.getInstance(this.getModel(), this.context);
    }


    public Class<?> getUpdateRequestBodyDTO() {
        return getDTO();
    }

    public Class<?> getUpdateResponseBodyDTO() {
        return getDTO();
    }

    public SerializerConfig configUpdateResponseSerializerFields() {
        return SerializerConfig.fromDTO(getUpdateResponseBodyDTO());
    }

    protected Filter configLookupFilter() {
        return new Filter("id", FilterOperation.EQUAL, FieldType.INTEGER);
    }


    @PutMapping("/{lookup}")
    public ResponseEntity<ObjectNode> update(@PathVariable(name = "lookup") Object lookupValue, HttpServletRequest request) throws Throwable {

        return this.update(this, lookupValue, request);
    }

    @PatchMapping("/{lookup}")
    public ResponseEntity<ObjectNode> patch(@PathVariable(name = "lookup") Object lookupValue, HttpServletRequest request) throws Throwable {
        return partialUpdate(this, lookupValue, request);
    }


    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("put") || handlerMethod.getMethod().getName().equals("patch")) {
            generateUpdateSchema(operation, getLookupFilter(), getUpdateRequestBodyDTO(), getUpdateResponseBodyDTO());
        }
    }


}
