package io.github.nikanique.springrestframework.swagger;

import io.github.nikanique.springrestframework.filter.Filter;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

@SuppressWarnings("rawtypes")
public interface DeleteSchemaGenerator {

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
