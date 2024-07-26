package io.github.nikanique.springrestframework.filter;

import io.github.nikanique.springrestframework.common.FieldType;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@NoArgsConstructor
@Data
@Accessors(chain = true, fluent = false)
@Builder
public class Filter implements Comparable<Filter> {
    private String name;

    private String modelFieldName;
    private FilterOperation operation;
    private FieldType fieldType;

    private String helpText;

    public Filter(String name, FilterOperation filterOperation, FieldType fieldType) {
        this.name = name;
        this.operation = filterOperation;
        this.fieldType = fieldType;
    }

    public Filter(String name, FilterOperation filterOperation, FieldType fieldType, String helpText) {
        this.name = name;
        this.operation = filterOperation;
        this.fieldType = fieldType;
        this.helpText = helpText;
    }

    public Filter(String name, String modelFieldName, FilterOperation filterOperation, FieldType fieldType) {
        this.name = name;
        this.operation = filterOperation;
        this.fieldType = fieldType;
        this.modelFieldName = modelFieldName;

    }

    public Filter(String name, String modelFieldName, FilterOperation filterOperation, FieldType fieldType, String helpText) {
        this.name = name;
        this.operation = filterOperation;
        this.fieldType = fieldType;
        this.modelFieldName = modelFieldName;
        this.helpText = helpText;

    }


    @Override
    public int compareTo(Filter other) {
        return this.name.compareTo(other.name);
    }
}
