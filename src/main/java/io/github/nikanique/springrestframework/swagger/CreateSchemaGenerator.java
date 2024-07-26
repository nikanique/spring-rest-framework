package io.github.nikanique.springrestframework.swagger;

import io.github.nikanique.springrestframework.common.EndpointType;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;

public interface CreateSchemaGenerator {

    default void generateCreateSchema(Operation operation, Class<?> createRequestBodyDTO, Class<?> createResponseDTO) {
        Schema<?> requestBodySchema = SwaggerSchemaGenerator.generateSchema(createRequestBodyDTO, EndpointType.WRITE);
        Content content = new Content().addMediaType("application/json", new MediaType().schema(requestBodySchema));

        operation.requestBody(new io.swagger.v3.oas.models.parameters.RequestBody().content(content));

        // Generate Response schema
        Schema<?> responseSchema = SwaggerSchemaGenerator.generateSchema(createResponseDTO, EndpointType.READ);
        ApiResponse response = new ApiResponse().content(new Content().addMediaType("application/json",
                new MediaType().schema(responseSchema)));
        operation.responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                .addApiResponse("201", response));
    }
}
