package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.orm.EntityBuilder;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.CommandService;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.swagger.CreateSchemaGenerator;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.method.HandlerMethod;

import java.io.BufferedReader;
import java.io.IOException;

@Getter
public abstract class CreateController<Model, ID, ModelRepository extends JpaRepository<Model, ID>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements CreateSchemaGenerator {


    final private SerializerConfig createResponseSerializerConfig;
    private EntityBuilder<Model> entityHelper;
    private CommandService<Model, ID> commandService;
    private QueryService<Model> queryService;


    @Autowired
    public CreateController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ModelRepository repository) {
        super(repository);
        this.createResponseSerializerConfig = configCreateSerializerFields();
    }

    private static String getRequestBody(HttpServletRequest request) throws IOException {
        StringBuilder sb = new StringBuilder();
        try (BufferedReader reader = request.getReader()) {
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
        }
        return sb.toString();
    }

    @PostConstruct
    private void postConstruct() {
        this.commandService = CommandService.getInstance(this.getModel(), this.repository, this.context);
        this.entityHelper = EntityBuilder.getInstance(this.getModel(), this.context);
    }

    protected Class<?> getCreateRequestBodyDTO() {
        return getDTO();
    }

    protected Class<?> getCreateResponseBodyDTO() {
        return getDTO();
    }


    public SerializerConfig configCreateSerializerFields() {
        return SerializerConfig.fromDTO(getCreateResponseBodyDTO());
    }

    @PostMapping("/")
    public ResponseEntity<ObjectNode> create(HttpServletRequest request) throws IOException {
        String requestBody = getRequestBody(request);
        Object dto = this.serializer.deserialize(requestBody, getCreateRequestBodyDTO());
        Model entity = this.getEntityHelper().fromDto(dto, this.getCreateRequestBodyDTO());
        entity = commandService.create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                serializer.serialize(entity, getCreateResponseSerializerConfig())
        );
    }


    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("create")) {
            generateCreateSchema(operation, getCreateRequestBodyDTO(), getCreateResponseBodyDTO());
        }
    }


}
