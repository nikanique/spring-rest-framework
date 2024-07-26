package io.github.nikanique.springrestframework.swagger;


import io.github.nikanique.springrestframework.common.EndpointType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.web.responses.PagedResponse;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.parameters.Parameter;
import io.swagger.v3.oas.models.responses.ApiResponse;

import java.util.Set;

@SuppressWarnings("rawtypes")
public interface ListSchemaGenerator {

    default void generateListSchema(Operation operation, Set<Filter> filterList, Class<?> listResponseDTO) {
        for (Filter filter : filterList) {
            if (filter.getOperation().equals(FilterOperation.BETWEEN)) {
                String fromParameterName = filter.getName() + "From";
                String toParameterName = filter.getName() + "To";
                operation.addParametersItem(new Parameter().name(fromParameterName).in("query")
                        .schema(new Schema().type(filter.getFieldType().toString().toLowerCase()))
                        .description(filter.getHelpText() == null ? "Filter operator :" + FilterOperation.GREATER_OR_EQUAL.name() : filter.getHelpText()));
                operation.addParametersItem(new Parameter().name(toParameterName).in("query")
                        .schema(new Schema().type(filter.getFieldType().toString().toLowerCase()))
                        .description(filter.getHelpText() == null ? "Filter operator :" + FilterOperation.LESS_OR_EQUAL.name() : filter.getHelpText()));
            } else if (filter.getOperation().equals(FilterOperation.IN)) {
                operation.addParametersItem(new Parameter().name(filter.getName()).in("query")
                        .schema(new Schema().type("string"))
                        .description(filter.getHelpText() == null ? "Filter operator :" + filter.getOperation().name() : filter.getHelpText()));
            } else {
                operation.addParametersItem(new Parameter().name(filter.getName()).in("query")
                        .schema(new Schema().type(filter.getFieldType().toString().toLowerCase()))
                        .description(filter.getHelpText() == null ? "Filter operator :" + filter.getOperation().name() : filter.getHelpText()));
            }
        }
        // Generate Response schema
        Schema<?> responseSchema = SwaggerSchemaGenerator.generatePagedResponseSchema(PagedResponse.class, listResponseDTO, EndpointType.READ);
        ApiResponse response = new ApiResponse().content(new Content().addMediaType("application/json",
                new MediaType().schema(responseSchema)));
        operation.responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                .addApiResponse("200", response));
    }
}
