package io.github.nikanique.springrestframework.web.controllers;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.dto.DtoManager;
import io.github.nikanique.springrestframework.filter.FilterSet;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.serializer.SerializerConfig;
import io.github.nikanique.springrestframework.services.QueryService;
import io.github.nikanique.springrestframework.web.responses.PagedResponse;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Sort;
import org.springframework.http.ResponseEntity;

import java.lang.reflect.Method;
import java.util.List;

public interface IListController<Model> {

    QueryService<Model> getQueryService();

    Method getQueryMethod();

    SerializerConfig getListSerializerConfig();

    FilterSet getFilterSet();

    default ResponseEntity<PagedResponse<ObjectNode>> list(BaseGenericController controller, HttpServletRequest request, int page, int size, String sortBy, Sort.Direction direction) throws Throwable {
        List<SearchCriteria> searchCriteriaList = SearchCriteria.fromUrlQuery(request, getFilterSet());
        searchCriteriaList = controller.filterByRequest(request, searchCriteriaList);
        String sortColumn = DtoManager.mapFieldToDBColumn(sortBy, controller.getDTO());

        Page<Object> entityPage = getQueryService().getPagedlist(searchCriteriaList, page, size, direction, sortColumn, getQueryMethod());
        List<ObjectNode> dtoList = entityPage.map(entity -> controller.getSerializer().serialize(entity, getListSerializerConfig())).getContent();
        PagedResponse<ObjectNode> response = new PagedResponse<>(dtoList, entityPage.getTotalElements());
        return ResponseEntity.ok(response);
    }
}
