package io.github.nikanique.springrestframework.orm;


import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.exceptions.ValidationException;
import io.github.nikanique.springrestframework.filter.Filter;
import io.github.nikanique.springrestframework.filter.FilterOperation;
import io.github.nikanique.springrestframework.filter.FilterSet;
import jakarta.servlet.http.HttpServletRequest;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.web.bind.ServletRequestUtils;

import java.time.LocalDateTime;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@AllArgsConstructor
@NoArgsConstructor
@Data
public class SearchCriteria {
    private String key;
    private FilterOperation filterOperation;
    private Object value;
    private FieldType fieldType;

    public static List<SearchCriteria> fromValue(Object lookupValue, Filter filter) {
        List<SearchCriteria> searchCriteriaList = new ArrayList<>();
        SearchCriteria searchCriteria = new SearchCriteria(
                filter.getModelFieldName() == null ?
                        filter.getName() :
                        filter.getModelFieldName(),
                filter.getOperation(),
                lookupValue,
                filter.getFieldType());
        searchCriteriaList.add(searchCriteria);
        return searchCriteriaList;
    }

    public static List<SearchCriteria> extendSearchCriteria(List<SearchCriteria> searchCriteriaList, Object value, Filter filter) {
        SearchCriteria searchCriteria = new SearchCriteria(
                filter.getModelFieldName() == null ?
                        filter.getName() :
                        filter.getModelFieldName(),
                filter.getOperation(),
                value, filter.getFieldType());
        searchCriteriaList.add(searchCriteria);
        return searchCriteriaList;
    }

    public static List<SearchCriteria> fromFilterList(Map<String, String> filterValues, FilterSet filterSet) throws ValidationException {
        List<SearchCriteria> searchCriteriaList = new ArrayList<>();

        for (Filter filter : filterSet.getFilters()) {
            String name = filter.getName();
            String modelFieldName = filter.getModelFieldName() == null ? filter.getName() : filter.getModelFieldName();
            String value;
            if (filter.getOperation().equals(FilterOperation.BETWEEN)) {
                String fromParameterName = name + "From";
                String toParameterName = name + "To";
                String fromValue = filterValues.get(fromParameterName);
                String toValue = filterValues.get(toParameterName);
                if (fromValue == null ^ toValue == null) {
                    throw new ValidationException(
                            fromValue == null ? fromParameterName : toParameterName,
                            "Both " + fromParameterName + " and " + toParameterName + " must be present.");
                }
                if (fromValue != null) {
                    Object parsedFromValue = extractAndValidateValue(fromParameterName, filter, fromValue);
                    Object parsedToValue = extractAndValidateValue(toParameterName, filter, toValue);
                    SearchCriteria fromSearchCriteria = new SearchCriteria(modelFieldName, FilterOperation.GREATER_OR_EQUAL, parsedFromValue, filter.getFieldType());
                    searchCriteriaList.add(fromSearchCriteria);
                    SearchCriteria toSearchCriteria = new SearchCriteria(modelFieldName, FilterOperation.LESS_OR_EQUAL, parsedToValue, filter.getFieldType());
                    searchCriteriaList.add(toSearchCriteria);
                }

            } else {
                value = filterValues.get(name);
                if (value != null) {
                    Object parsedValue = extractAndValidateValue(name, filter, value);
                    SearchCriteria searchCriteria = new SearchCriteria(modelFieldName, filter.getOperation(), parsedValue, filter.getFieldType());
                    searchCriteriaList.add(searchCriteria);
                }
            }

        }

        return searchCriteriaList;
    }

    private static Object extractAndValidateValue(String name, Filter filter, String value) throws ValidationException {
        try {
            // If the operation is IN, then we only validate the comma separated items, and return the items string.
            if (filter.getOperation() == FilterOperation.IN) {
                List<String> values = Arrays.asList(value.split(","));
                // Validating the values
                values.parallelStream()
                        .forEach(v -> parseValue(filter, v));

                return value;
            }
            // Parse simple value
            return parseValue(filter, value);
        } catch (DateTimeParseException | IllegalArgumentException e) {
            throw new ValidationException(name, value + " is invalid value for type " + filter.getFieldType());
        }
    }

    private static Object parseValue(Filter filter, String value) {
        switch (filter.getFieldType()) {
            case INTEGER:
                return Integer.parseInt(value);
            case STRING:
                return value;
            case DATE_TIME:
                return LocalDateTime.parse(value);
            case TIMESTAMP:
                return java.sql.Timestamp.valueOf(value);
            case BOOLEAN:
                return Boolean.parseBoolean(value);
            case DOUBLE:
                return Double.parseDouble(value);
            case LONG:
                return Long.parseLong(value);
            case FLOAT:
                return Float.parseFloat(value);
            default:
                throw new IllegalArgumentException("Invalid fieldType");
        }
    }

    public static List<SearchCriteria> fromUrlQuery(HttpServletRequest request, FilterSet filterSet) throws ValidationException {
        List<SearchCriteria> searchCriteriaList = new ArrayList<>();

        for (Filter filter : filterSet.getFilters()) {
            String name = filter.getName();
            String modelFieldName = filter.getModelFieldName() == null ? filter.getName() : filter.getModelFieldName();
            String value;
            if (filter.getOperation().equals(FilterOperation.BETWEEN)) {
                String fromParameterName = name + "From";
                String toParameterName = name + "To";
                String fromValue = ServletRequestUtils.getStringParameter(request, fromParameterName, null);
                String toValue = ServletRequestUtils.getStringParameter(request, toParameterName, null);
                if (fromValue == null ^ toValue == null) {
                    throw new ValidationException(
                            fromValue == null ? fromParameterName : toParameterName,
                            "Both " + fromParameterName + " and " + toParameterName + " must be present.");
                }
                if (fromValue != null) {
                    Object parsedFromValue = extractAndValidateValue(fromParameterName, filter, fromValue);
                    Object parsedToValue = extractAndValidateValue(toParameterName, filter, toValue);
                    SearchCriteria fromSearchCriteria = new SearchCriteria(modelFieldName, FilterOperation.GREATER_OR_EQUAL, parsedFromValue, filter.getFieldType());
                    searchCriteriaList.add(fromSearchCriteria);
                    SearchCriteria toSearchCriteria = new SearchCriteria(modelFieldName, FilterOperation.LESS_OR_EQUAL, parsedToValue, filter.getFieldType());
                    searchCriteriaList.add(toSearchCriteria);
                }

            } else {
                value = ServletRequestUtils.getStringParameter(request, name, null);
                if (value != null) {
                    Object parsedValue = extractAndValidateValue(name, filter, value);
                    SearchCriteria searchCriteria = new SearchCriteria(modelFieldName, filter.getOperation(), parsedValue, filter.getFieldType());
                    searchCriteriaList.add(searchCriteria);
                }
            }

        }

        return searchCriteriaList;
    }

    public static String generateSqlWhereClause(List<SearchCriteria> searchCriteriaList) {
        if (searchCriteriaList == null || searchCriteriaList.isEmpty()) {
            return "";
        }
        List<String> predicates = searchCriteriaList.stream()
                .map(SearchCriteria::convertCriteriaToSql)
                .collect(Collectors.toList());
        return "WHERE " + String.join(" AND ", predicates);
    }

    public String convertCriteriaToSql() {
        String column = this.getKey();
        FilterOperation operation = this.getFilterOperation();
        Object value = this.getValue();

        switch (operation) {
            case EQUAL:
                return column + " = " + formatValue(value, this.getFieldType());
            case GREATER:
                return column + " > " + formatValue(value, this.getFieldType());
            case GREATER_OR_EQUAL:
                return column + " >= " + formatValue(value, this.getFieldType());
            case LESS:
                return column + " < " + formatValue(value, this.getFieldType());
            case LESS_OR_EQUAL:
                return column + " <= " + formatValue(value, this.getFieldType());
            case CONTAINS:
                return column + " LIKE " + ("'%" + value.toString() + "%'");
            case IN:
                if (value instanceof String) {
                    String inClause = Arrays.stream(((String) value).split(","))
                            .map(item -> formatValue(item, this.getFieldType()))
                            .collect(Collectors.joining(", "));
                    return column + " IN (" + inClause + ")";
                }
                throw new IllegalArgumentException("IN operation requires a list of values.");
            default:
                throw new IllegalArgumentException("Unknown filter operation: " + operation);
        }
    }

    private String formatValue(Object value, FieldType fieldType) {
        if (
                fieldType.isDateTime() || fieldType == FieldType.STRING
        ) {
            return "'" + value + "'";
        }

        return value.toString();

    }
}
