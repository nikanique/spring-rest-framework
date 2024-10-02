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
