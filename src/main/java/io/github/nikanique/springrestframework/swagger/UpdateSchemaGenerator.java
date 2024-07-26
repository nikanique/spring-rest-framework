package io.github.nikanique.springrestframework.swagger;


import io.github.nikanique.springrestframework.common.EndpointType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

@SuppressWarnings("rawtypes")
public interface UpdateSchemaGenerator {

    default void generateUpdateSchema(Operation operation, Filter lookupValueFilter, Class<?> updateRequestBodyDTO, Class<?> updateResponseDTO) {
        Schema<?> requestBodySchema = SwaggerSchemaGenerator.generateSchema(updateRequestBodyDTO, EndpointType.WRITE);
        Content content = new Content().addMediaType("application/json", new MediaType().schema(requestBodySchema));

        operation.getParameters().get(0).schema(new Schema().type(lookupValueFilter.getFieldType().toString().toLowerCase()))
                .description(lookupValueFilter.getHelpText() == null ?
                        (lookupValueFilter.getName() == null ? "" :
                                "Field: " + lookupValueFilter.getName()) + " Filter operator :" + lookupValueFilter.getOperation().name() :
                        lookupValueFilter.getHelpText());
        operation.requestBody(new io.swagger.v3.oas.models.parameters.RequestBody().content(content));

        Schema<?> responseSchema = SwaggerSchemaGenerator.generateSchema(updateResponseDTO, EndpointType.READ);
        ApiResponse response = new ApiResponse().content(new Content().addMediaType("application/json",
                new MediaType().schema(responseSchema)));
        operation.responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                .addApiResponse("201", response));
    }
}
