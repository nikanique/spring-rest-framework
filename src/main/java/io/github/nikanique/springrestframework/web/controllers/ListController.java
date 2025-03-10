package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.EndpointType;
import io.github.nikanique.springrestframework.dto.DtoManager;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.filter.FilterSet;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.swagger.SwaggerSchemaGenerator;
import io.github.nikanique.springrestframework.web.responses.ErrorResponse;
import io.github.nikanique.springrestframework.web.responses.PagedResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.Collections;
import java.util.List;
import java.util.Set;

/**
 * This interface provides methods for listing entities.
 *
 * @param <Model> The model you want to list the records of (eg. User)
 */
@SuppressWarnings("ALL")
public interface ListController<Model> {

    QueryService<Model> getQueryService();

    Method getQueryMethod();

    SerializerConfig getListSerializerConfig();

    FilterSet getFilterSet();

    Set<String> getAllowedOrderByFields();

    default Set<String> configAllowedOrderByFields() {
        return Collections.emptySet();
    }

    default ResponseEntity list(BaseGenericController controller, HttpServletRequest request, int page, int size, String sortBy, Sort.Direction direction) throws Throwable {
        if (!getAllowedOrderByFields().isEmpty() && !sortBy.isEmpty() && !getAllowedOrderByFields().contains(sortBy)) {
            return ResponseEntity.badRequest().body(new ErrorResponse("Sorting by " + sortBy + " is not allowed"));
        }

        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromUrlQuery(request, getFilterSet());
        searchCriteriaList = controller.filterByRequest(request, searchCriteriaList);
        String sortColumn = DtoManager.mapFieldToDBColumn(sortBy, controller.getDTO());

        Page<Object> entityPage = getQueryService().getPagedlist(searchCriteriaList, page, size, direction, sortColumn, getQueryMethod());
        List<ObjectNode> dtoList = entityPage.map(entity -> controller.getSerializer().serialize(entity, getListSerializerConfig())).getContent();
        PagedResponse<ObjectNode> response = new PagedResponse<>(dtoList, entityPage.getTotalElements(), "OK");
        return ResponseEntity.ok(response);
    }

    default void generateListSchema(Operation operation, Set<Filter> filterList, Class<?> listResponseDTO) {
        for (Filter filter : filterList) {
            if (filter.getOperation().equals(FilterOperation.BETWEEN)) {
                String fromParameterName = filter.getName() + "From";
                String toParameterName = filter.getName() + "To";
                operation.addParametersItem(new Parameter().name(fromParameterName).in("query")
                        .schema(new Schema().type(filter.getFieldType().toString().toLowerCase()))
                        .required(filter.isRequired())
                        .description(filter.getHelpText() == null ? "Filter operator :" + FilterOperation.GREATER_OR_EQUAL.name() : filter.getHelpText()));
                operation.addParametersItem(new Parameter().name(toParameterName).in("query")
                        .schema(new Schema().type(filter.getFieldType().toString().toLowerCase()))
                        .required(filter.isRequired())
                        .description(filter.getHelpText() == null ? "Filter operator :" + FilterOperation.LESS_OR_EQUAL.name() : filter.getHelpText()));
            } else if (filter.getOperation().equals(FilterOperation.IN)) {
                operation.addParametersItem(new Parameter().name(filter.getName()).in("query")
                        .schema(new Schema().type("string"))
                        .required(filter.isRequired())
                        .description(filter.getHelpText() == null ? "Filter operator :" + filter.getOperation().name() : filter.getHelpText()));
            } else {
                operation.addParametersItem(new Parameter().name(filter.getName()).in("query")
                        .schema(new Schema().type(filter.getFieldType().toString().toLowerCase()))
                        .required(filter.isRequired())
                        .description(filter.getHelpText() == null ? "Filter operator :" + filter.getOperation().name() : filter.getHelpText()));
            }
        }
        // Generate Response schema
        Schema<?> responseSchema = SwaggerSchemaGenerator.generatePagedResponseSchema(PagedResponse.class, listResponseDTO, EndpointType.READ);
        ApiResponse response = new ApiResponse().content(new Content().addMediaType("application/json",
                new MediaType().schema(responseSchema)));
        // Generate error Response schema
        Schema<?> errorResponseSchema = SwaggerSchemaGenerator.generateSchema(ErrorResponse.class, EndpointType.READ);
        ApiResponse errorResponse = new ApiResponse().content(new Content().addMediaType("application/json",
                new MediaType().schema(errorResponseSchema)));

        operation.responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                .addApiResponse("200", response).addApiResponse("400", errorResponse));
    }
}
