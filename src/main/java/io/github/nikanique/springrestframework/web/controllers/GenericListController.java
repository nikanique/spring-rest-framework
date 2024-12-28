package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.filter.FilterSet;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.utilities.MethodReflectionHelper;
import io.github.nikanique.springrestframework.web.responses.PagedResponse;
import io.swagger.v3.oas.models.Operation;
import jakarta.annotation.PostConstruct;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Getter;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;
import java.util.Set;
import java.util.TreeSet;

/**
 * The GenericListController class is a generic controller designed for use in Spring Boot applications for listing model's records.
 * It provides a common implementation for listing records from a repository with a variety of filtering options.
 * This class is particularly useful when you need to build REST APIs for managing database records where listing
 * and filtering functionalities are required. It exposes endpoint with GET method.
 * <p>
 * Example:
 * <pre>
 *     {@code
 * @RequestMapping("/student")
 * @RestController
 * @Tag(name = "Student")
 * public class StudentController extends GenericListController<Student, Long, StudentRepository> {
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
public abstract class GenericListController<Model, ID, ModelRepository extends JpaRepository<Model, ID> & JpaSpecificationExecutor<Model>>
        extends BaseGenericController<Model, ID, ModelRepository>
        implements ListController<Model> {

    final private SerializerConfig listSerializerConfig;
    final private FilterSet filterSet;
    final private Method queryMethod;
    private final Set<String> allowedOrderByFields;
    private QueryService<Model> queryService;

    public GenericListController(ModelRepository repository) throws NoSuchMethodException {
        super(repository);
        this.filterSet = configFilterSet();
        this.listSerializerConfig = configListSerializer();
        this.queryMethod = MethodReflectionHelper.findRepositoryMethod(getQueryMethodName(), repository);
        this.allowedOrderByFields = configAllowedOrderByFields();

    }

    protected String getQueryMethodName() {
        return "findAll";
    }

    @PostConstruct
    private void postConstruct() {
        this.queryService = QueryService.getInstance(this.getModel(), this.repository, this.context);
    }


    protected Class<?> getListResponseDTO() {
        return getDTO();
    }

    protected FilterSet configFilterSet() {
        return new FilterSet(new TreeSet<>());
    }

    public SerializerConfig configListSerializer() {
        return SerializerConfig.fromDTO(getListResponseDTO());
    }

    @GetMapping("/")
    public ResponseEntity<PagedResponse<ObjectNode>> get(
            HttpServletRequest request,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(defaultValue = "") String sortBy,
            @RequestParam(defaultValue = "ASC") Sort.Direction direction) throws Throwable {
        this.authorizeRequest(request);
        return this.list(this, request, page, size, sortBy, direction);
    }


    public void customizeOperationForController(Operation operation, HandlerMethod handlerMethod) {
        if (handlerMethod.getMethod().getName().equals("get")) {
            this.generateListSchema(operation, this.getFilterSet().getFilters(), this.getListResponseDTO());
        }
    }


}
