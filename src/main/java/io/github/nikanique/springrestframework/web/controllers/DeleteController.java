package io.github.nikanique.springrestframework.web.controllers;

import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.services.CommandService;
import io.github.nikanique.springrestframework.services.QueryService;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;

public interface DeleteController<Model, ID> {
    QueryService<Model> getQueryService();

    CommandService<Model, ID> getCommandService();

    Filter getLookupFilter();

    default ResponseEntity<Void> deleteObject(BaseGenericController controller, HttpServletRequest request, Object lookupValue) {
        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromValue(lookupValue, this.getLookupFilter());
        searchCriteriaList = controller.filterByRequest(request, searchCriteriaList);

        // Retrieve the entity using specification
        Optional<Object> optionalEntity = this.getQueryService().getObject(searchCriteriaList);

        if (!optionalEntity.isPresent()) {
            return ResponseEntity.notFound().build();
        }

        this.getCommandService().delete((Model) optionalEntity.get());
        return ResponseEntity.noContent().build();
    }

    default void generateDeleteSchema(Operation operation, Filter lookupValueFilter) {
        operation.setRequestBody(null);

        operation.getParameters().get(0).schema(new Schema().type(lookupValueFilter.getFieldType().toString().toLowerCase()))
                .description(lookupValueFilter.getHelpText() == null ?
                        (lookupValueFilter.getName() == null ? "" :
                                "Field: " + lookupValueFilter.getName()) + " Filter operator :" + lookupValueFilter.getOperation().name() :
                        lookupValueFilter.getHelpText());

        ApiResponse response = new ApiResponse().description("No Content");
        operation.responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                .addApiResponse("204", response));
    }
}
