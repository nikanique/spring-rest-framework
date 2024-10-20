package io.github.nikanique.springrestframework.web.controllers;

import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.services.CommandService;
import io.github.nikanique.springrestframework.services.QueryService;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.PostConstruct;
import lombok.Getter;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.web.method.HandlerMethod;

/**
 * The GenericDeleteController class is a generic controller designed for use in Spring Boot applications for deleting model's records.
 * It provides a common implementation for deleting records. It exposes endpoint with DELETE method.
 * This class is particularly useful when you need to build REST APIs for creating records.
 * <p>
 * Example:
 * <pre>
 *     {@code
 * @RequestMapping("/student")
 * @RestController
 * @Tag(name = "Student")
 * public class StudentController extends GenericDeleteController<Student, Long, StudentRepository> {
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
public abstract class GenericDeleteController<Model, ID, ModelRepository extends JpaRepository<Model, ID> & JpaSpecificationExecutor<Model>>
        extends BaseGenericController<Model, ID, ModelRepository> implements DeleteController<Model, ID> {

    final private Filter lookupFilter;
    private CommandService<Model, ID> commandService;
    private QueryService<Model> queryService;

    public GenericDeleteController(@SuppressWarnings("SpringJavaInjectionPointsAutowiringInspection") ModelRepository repository) {
        super(repository);

        this.lookupFilter = configLookupFilter();
    }


    @PostConstruct
    private void postConstruct() {
        this.commandService = CommandService.getInstance(this.getModel(), this.repository, this.context);
        this.queryService = QueryService.getInstance(this.getModel(), this.repository, this.context);
    }

    protected Filter configLookupFilter() {
        return new Filter("id", FilterOperation.EQUAL, FieldType.INTEGER);
    }

    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("delete")) {
            generateDeleteSchema(operation, getLookupFilter());
        }
    }

}
