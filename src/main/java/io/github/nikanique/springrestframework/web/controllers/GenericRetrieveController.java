package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.utilities.MethodReflectionHelper;
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

import java.lang.reflect.Method;

/**
 * The GenericRetrieveController class is a generic controller designed for use in Spring Boot applications for retrieving model's records.
 * It provides a common implementation for retrieving a single record from a repository with a variety of filtering options.
 * This class is particularly useful when you need to build REST APIs for managing database records where retrieval
 * functionalities are required. It exposes endpoint with GET method with path variable.
 * <p>
 * Example:
 * <pre>
 *     {@code
 * @RequestMapping("/student")
 * @RestController
 * @Tag(name = "Student")
 * public class StudentController extends GenericRetrieveController<Student, Long, StudentRepository> {
 *     public StudentController(StudentRepository repository) {
 *         super(repository);
 *     }
 *
 *     @Override
 *     protected Class<?> getDTO() {
 *         return StudentDto.class;
 *     }
 * }
 * }
 * </pre>
 *
 * @param <Model>           The class type of the entity (e.g., Student).
 * @param <ID>              The type of the model’s identifier (e.g., Long).
 * @param <ModelRepository> The repository interface extending JpaRespository and JpaSpecificationExecutor (e.g., StudentRepository).
 */
@Getter
public abstract class GenericRetrieveController<Model, ID, ModelRepository extends JpaRepository<Model, ID> & JpaSpecificationExecutor<Model>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements RetrieveController {

    final private SerializerConfig retrieveSerializerConfig;
    final private Filter lookupFilter;
    final private Method queryMethod;
    private QueryService<Model> queryService;

    public GenericRetrieveController(ModelRepository repository) throws NoSuchMethodException {
        super(repository);
        this.retrieveSerializerConfig = configRetrieveSerializer();
        this.lookupFilter = configLookupFilter();
        this.queryMethod = MethodReflectionHelper.findRepositoryMethod(getQueryMethodName(), repository);

    }

    protected String getQueryMethodName() {
        return "findAll";
    }

    @PostConstruct
    private void postConstruct() {
        this.queryService = QueryService.getInstance(this.getModel(), this.repository, this.context);
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
            @PathVariable(name = "lookup") Object lookupValue) throws Throwable {

        return this.retrieve(this, request, lookupValue);
    }


    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("getByLookupValue")) {
            this.generateRetrieveSchema(operation, this.getLookupFilter(), this.getRetrieveResponseDTO());
        }
    }


}
