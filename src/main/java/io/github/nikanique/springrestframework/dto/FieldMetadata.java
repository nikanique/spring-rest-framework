package io.github.nikanique.springrestframework.dto;

import io.github.nikanique.springrestframework.annotation.*;
import lombok.Getter;

import java.lang.invoke.MethodHandle;
import java.lang.reflect.Field;

@Getter
public class FieldMetadata {
    private final Field field;

    private final Class<?> fieldType;
    private final FieldValidation validation;
    private final Expose expose;
    private final ReadOnly readOnly;
    private final WriteOnly writeOnly;
    private final ReferencedModel referencedModel;
    private final MethodHandle getterMethodHandle;

    public FieldMetadata(Field field, Class<?> fieldType, FieldValidation validation, Expose expose, ReadOnly readOnly, WriteOnly writeOnly, ReferencedModel referencedModel, MethodHandle getterMethodHandle) {
        this.field = field;
        this.fieldType = fieldType;
        this.validation = validation;
        this.expose = expose;
        this.readOnly = readOnly;
        this.writeOnly = writeOnly;
        this.referencedModel = referencedModel;
        this.getterMethodHandle = getterMethodHandle;
    }
}
