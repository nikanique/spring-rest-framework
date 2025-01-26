package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.EndpointType;
import io.github.nikanique.springrestframework.orm.EntityBuilder;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.CommandService;
import io.github.nikanique.springrestframework.swagger.SwaggerSchemaGenerator;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/**
 * This interface provides methods for creating entities.
 *
 * @param <Model> The model
 * @param <ID>    Type of model's primary key (id)
 */
public interface CreateController<Model, ID> extends RequestBodyProvider {
    SerializerConfig getCreateResponseSerializerConfig();

    Class<?> getCreateRequestBodyDTO();

    Class<?> getCreateResponseBodyDTO();

    EntityBuilder<Model> getEntityHelper();

    CommandService<Model, ID> getCommandService();

    default SerializerConfig configCreateSerializerFields() {
        return SerializerConfig.fromDTO(getCreateResponseBodyDTO());
    }


    default ResponseEntity<ObjectNode> create(BaseGenericController controller, HttpServletRequest request) throws Throwable {
        String requestBody = this.getRequestBody(request);
        Object dto = controller.getSerializer().deserialize(requestBody, getCreateRequestBodyDTO(), true);
        return performCreate(controller, dto);
    }

    default ResponseEntity<ObjectNode> performCreate(BaseGenericController controller, Object dto) {
        Model entity = this.getEntityHelper().fromDto(dto, this.getCreateRequestBodyDTO());
        entity = getCommandService().create(entity);
        return ResponseEntity.status(HttpStatus.CREATED).body(
                controller.getSerializer().serialize(entity, getCreateResponseSerializerConfig())
        );
    }

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
