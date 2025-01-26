package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.EndpointType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.CommandService;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.swagger.SwaggerSchemaGenerator;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

import java.util.List;
import java.util.Optional;
import java.util.Set;

/**
 * This interface provides methods for updating entities.
 *
 * @param <Model> The model you want to list the records of (eg. User)
 * @param <ID>    Type of model's primary key (id)
 */
public interface UpdateController<Model, ID> extends RequestBodyProvider {

    Class<?> getUpdateRequestBodyDTO();

    SerializerConfig getUpdateResponseSerializerConfig();

    Filter getLookupFilter();

    QueryService<Model> getQueryService();

    CommandService<Model, ID> getCommandService();

    default ResponseEntity<ObjectNode> update(BaseGenericController controller, Object lookupValue, HttpServletRequest request) throws Throwable {
        // Create search criteria from lookup value
        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromValue(lookupValue, this.getLookupFilter());
        searchCriteriaList = controller.filterByRequest(request, searchCriteriaList);

        // Retrieve the entity using specification
        Optional<Object> optionalEntity = getObject(searchCriteriaList);
        if (optionalEntity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String requestBody = this.getRequestBody(request);
        Object dto = controller.getSerializer().deserialize(requestBody, this.getUpdateRequestBodyDTO(), true);

        return performUpdate(controller, optionalEntity.get(), dto);
    }

    default ResponseEntity<ObjectNode> performUpdate(BaseGenericController controller, Object entity, Object dto) throws Throwable {
        // Update the entity fields except the lookup field
        Model entityFromDB = this.getCommandService().update((Model) entity, dto, this.getLookupFilter().getName(), this.getUpdateRequestBodyDTO());

        // Return the updated entity
        return ResponseEntity.status(HttpStatus.OK).body(
                controller.getSerializer().serialize(entityFromDB, this.getUpdateResponseSerializerConfig())
        );
    }

    default Optional<Object> getObject(List<SearchCriteria> searchCriteriaList) {
        Optional<Object> optionalEntity = this.getQueryService().getObject(searchCriteriaList);
        return optionalEntity;
    }

    default ResponseEntity<ObjectNode> partialUpdate(BaseGenericController controller, Object lookupValue, HttpServletRequest request) throws Throwable {
        // Create search criteria from lookup value
        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromValue(lookupValue, this.getLookupFilter());
        searchCriteriaList = controller.filterByRequest(request, searchCriteriaList);

        // Retrieve the entity using specification
        Optional<Object> optionalEntity = getObject(searchCriteriaList);
        if (optionalEntity.isEmpty()) {
            return ResponseEntity.notFound().build();
        }

        String requestBody = getRequestBody(request);
        Set<String> presentFields = controller.getSerializer().getPresentFields(requestBody);
        Object dto = controller.getSerializer().deserialize(requestBody, this.getUpdateRequestBodyDTO(), true, presentFields);

        return performPartialUpdate(controller, optionalEntity.get(), dto, presentFields);
    }

    default ResponseEntity<ObjectNode> performPartialUpdate(BaseGenericController controller, Object entity, Object dto, Set<String> presentFields) throws Throwable {
        // Partially update the entity fields except the lookup field
        Model entityFromDB = this.getCommandService().update((Model) entity, dto, this.getLookupFilter().getName(), this.getUpdateRequestBodyDTO(), presentFields);

        // Return the updated entity
        return ResponseEntity.status(HttpStatus.OK).body(
                controller.getSerializer().serialize(entityFromDB, getUpdateResponseSerializerConfig())
        );
    }

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
