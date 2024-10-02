package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.EndpointType;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.swagger.SwaggerSchemaGenerator;
import io.swagger.v3.oas.models.Operation;
import io.swagger.v3.oas.models.media.Content;
import io.swagger.v3.oas.models.media.MediaType;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.responses.ApiResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.List;
import java.util.Optional;

public interface RetrieveController<Model> {

    QueryService<Model> getQueryService();

    Method getQueryMethod();

    SerializerConfig getRetrieveSerializerConfig();

    Filter getLookupFilter();

    default ResponseEntity<ObjectNode> retrieve(BaseGenericController controller, HttpServletRequest request, Object lookupValue) throws Throwable {
        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromValue(lookupValue, getLookupFilter());
        searchCriteriaList = controller.filterByRequest(request, searchCriteriaList);

        Optional<Object> optionalEntity = getQueryService().getObject(searchCriteriaList, getQueryMethod());
        return optionalEntity.map(entity -> ResponseEntity.ok(
                        controller.getSerializer().serialize(entity, getRetrieveSerializerConfig())
                ))
                .orElseGet(() -> ResponseEntity.notFound().build());
    }


    default void generateRetrieveSchema(Operation operation, Filter lookupValueFilter, Class<?> retrieveResponseDTO) {
        operation.getParameters().get(0).schema(new Schema().type(lookupValueFilter.getFieldType().toString().toLowerCase()))
                .description(lookupValueFilter.getHelpText() == null ?
                        (lookupValueFilter.getName() == null ? "" :
                                "Field: " + lookupValueFilter.getName()) + " Filter operator :" + lookupValueFilter.getOperation().name() :
                        lookupValueFilter.getHelpText());
        Schema<?> responseSchema = SwaggerSchemaGenerator.generateSchema(retrieveResponseDTO, EndpointType.READ);
        ApiResponse response = new ApiResponse().content(new Content().addMediaType("application/json",
                new MediaType().schema(responseSchema)));
        operation.responses(new io.swagger.v3.oas.models.responses.ApiResponses()
                .addApiResponse("200", response));
    }
}
