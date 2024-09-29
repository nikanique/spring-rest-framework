package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
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
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import java.io.IOException;

@Getter
public abstract class CreateController<Model, ID, ModelRepository extends JpaRepository<Model, ID>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements ICreateController<Model, ID> {

    final private SerializerConfig createResponseSerializerConfig;
    private EntityBuilder<Model> entityHelper;
    private CommandService<Model, ID> commandService;
    private QueryService<Model> queryService;

    @Autowired
    public CreateController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ModelRepository repository) {
        super(repository);
        this.createResponseSerializerConfig = configCreateSerializerFields();
    }

    @PostConstruct
    private void postConstruct() {
        this.commandService = CommandService.getInstance(this.getModel(), this.repository, this.context);
        this.entityHelper = EntityBuilder.getInstance(this.getModel(), this.context);
    }

    public Class<?> getCreateRequestBodyDTO() {
        return getDTO();
    }

    public Class<?> getCreateResponseBodyDTO() {
        return getDTO();
    }

    @PostMapping("/")
    public ResponseEntity<ObjectNode> post(HttpServletRequest request) throws IOException {
        return this.create(this, request);
    }

    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("post")) {
            this.generateCreateSchema(operation, getCreateRequestBodyDTO(), getCreateResponseBodyDTO());
        }
    }


}
