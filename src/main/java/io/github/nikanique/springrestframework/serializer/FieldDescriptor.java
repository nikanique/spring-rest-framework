package io.github.nikanique.springrestframework.serializer;

import io.github.nikanique.springrestframework.common.FieldType;
import lombok.Data;

@Data
public class FieldDescriptor {


    private FieldType fieldType;
    private String exposeName;
    private String format;

    private String source;
    private String methodName;


    public FieldDescriptor(FieldType fieldType) {
        this.fieldType = fieldType;
    }

    public FieldDescriptor(FieldType fieldType, String format) {
        this.fieldType = fieldType;
        this.format = format;
    }

    public FieldDescriptor(FieldType fieldType, String exposeName, String format) {
        this.fieldType = fieldType;
        this.exposeName = exposeName;
        this.format = format;
    }

    public FieldDescriptor(FieldType fieldType, String exposeName, String format, String methodName) {
        this.fieldType = fieldType;
        this.exposeName = exposeName;
        this.format = format;
        this.methodName = methodName;
    }

    public FieldDescriptor(FieldType fieldType, String exposeName, String format, String methodName, String source) {
        this.fieldType = fieldType;
        this.exposeName = exposeName;
        this.format = format;
        this.methodName = methodName;
        this.source = source;
    }

    public FieldDescriptor(String exposeName) {
        this.exposeName = exposeName;
    }

}
