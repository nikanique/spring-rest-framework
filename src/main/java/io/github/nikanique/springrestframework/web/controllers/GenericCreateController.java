package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.orm.EntityBuilder;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.CommandService;
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

/**
 * The GenericCreateController class is a generic controller designed for use in Spring Boot applications for creating model's records.
 * It provides a common implementation for creating records. It exposes endpoint with POST method.
 * This class is particularly useful when you need to build REST APIs for creating records.
 * <p>
 * Example:
 * <pre>
 *     {@code
 * @RequestMapping("/student")
 * @RestController
 * @Tag(name = "Student")
 * public class StudentController extends GenericCreateController<Student, Long, StudentRepository> {
 *     public StudentController(StudentRepository repository) {
 *         super(repository);
 *     }
 *
 *     @Override
 *     protected Class<?> getDTO() {
 *         return StudentDto.class;
 *     }
 * }
 * }</pre>
 *
 * @param <Model>
 * @param <ID>
 * @param <ModelRepository>
 */
@Getter
public abstract class GenericCreateController<Model, ID, ModelRepository extends JpaRepository<Model, ID>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements CreateController<Model, ID> {

    final private SerializerConfig createResponseSerializerConfig;
    private EntityBuilder<Model> entityHelper;
    private CommandService<Model, ID> commandService;

    @Autowired
    public GenericCreateController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ModelRepository repository) {
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
