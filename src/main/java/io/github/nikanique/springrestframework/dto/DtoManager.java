package io.github.nikanique.springrestframework.dto;


import io.github.nikanique.springrestframework.annotation.*;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.stream.Collectors;
import java.util.stream.Stream;

public class DtoManager {
    // Use ConcurrentHashMap for thread-safe caching
    private static final Map<Class<?>, Map<String, FieldMetadata>> fieldCache = new ConcurrentHashMap<>();

    // Method to get cached field metadata or create it if not present
    public static Map<String, FieldMetadata> getDtoByClassName(Class<?> clazz) {
        return fieldCache.computeIfAbsent(clazz, cls -> {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            return Stream.of(cls.getDeclaredFields())
                    .map(field -> {
                        MethodHandle getterMethodHandle = null;
                        try {
                            String getterName = "get" + Character.toUpperCase(field.getName().charAt(0)) + field.getName().substring(1);
                            MethodType methodType = MethodType.methodType(field.getType());
                            getterMethodHandle = lookup.findVirtual(cls, getterName, methodType);
                        } catch (NoSuchMethodException | IllegalAccessException e) {
                            // Log the exception or handle it as per your requirement
                        }
                        return new FieldMetadata(field,
                                field.getType(), field.getAnnotation(FieldValidation.class),
                                field.getAnnotation(Expose.class),
                                field.getAnnotation(ReadOnly.class),
                                field.getAnnotation(WriteOnly.class),
                                field.getAnnotation(ReferencedModel.class),
                                getterMethodHandle);
                    })
                    .collect(Collectors.toMap(fieldMetadata -> fieldMetadata.getField().getName(), fieldMetadata -> fieldMetadata));
        });
    }

}
