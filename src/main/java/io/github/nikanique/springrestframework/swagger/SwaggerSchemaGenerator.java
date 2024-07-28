package io.github.nikanique.springrestframework.swagger;


import io.github.nikanique.springrestframework.annotation.Expose;
import io.github.nikanique.springrestframework.annotation.ReadOnly;
import io.github.nikanique.springrestframework.annotation.WriteOnly;
import io.github.nikanique.springrestframework.common.EndpointType;
import io.github.nikanique.springrestframework.common.FieldType;
import io.swagger.v3.oas.models.media.ArraySchema;
import io.swagger.v3.oas.models.media.Schema;

import java.lang.reflect.Field;
import java.lang.reflect.ParameterizedType;
import java.sql.Timestamp;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@SuppressWarnings("rawtypes")
public class SwaggerSchemaGenerator {

    private static final Map<Class<?>, Class<?>> typeMapping = new HashMap<>();

    static {
        typeMapping.put(int.class, Integer.class);
        typeMapping.put(long.class, Long.class);
        typeMapping.put(float.class, Float.class);
        typeMapping.put(double.class, Double.class);
        typeMapping.put(boolean.class, Boolean.class);
        typeMapping.put(char.class, Character.class);
        typeMapping.put(byte.class, Byte.class);
        typeMapping.put(short.class, Short.class);
    }

    public static <T, U> Schema<T> generatePagedResponseSchema(Class<T> clazz, Class<U> resultClass, EndpointType endpointType) {
        Schema<T> schema = new Schema<>();
        schema.setType("object");
        schema.setName(clazz.getName());
        schema.setTitle(clazz.getSimpleName());
        Map<String, Schema> properties = new HashMap<>();
        for (Field field : clazz.getFields()) {
            Schema fieldSchema;
            if ("result".equals(field.getName())) {
                fieldSchema = new ArraySchema().items(generateSchema(resultClass, endpointType));
            } else {
                fieldSchema = generateFieldSchema(field, endpointType);
            }
            properties.put(field.getName(), fieldSchema);
        }
        schema.setProperties(properties);
        return schema;
    }

    // Function to generate schema based on class structure
    public static <T> Schema<T> generateSchema(Class<T> clazz, EndpointType endpointType) {
        Schema<T> schema = new Schema<>();
        schema.setType("object");
        schema.setName(clazz.getName());
        schema.setTitle(clazz.getSimpleName());
        Map<String, Schema> properties = new HashMap<>();
        for (Field field : clazz.getDeclaredFields()) {
            ReadOnly readOnlyAnnotation = field.getAnnotation(ReadOnly.class);
            WriteOnly writeOnlyAnnotation = field.getAnnotation(WriteOnly.class);
            if (endpointType.equals(EndpointType.WRITE) && readOnlyAnnotation != null) {
                continue;
            }
            if (endpointType.equals(EndpointType.READ) && writeOnlyAnnotation != null) {
                continue;
            }
            Expose exposeAnnotation = field.getAnnotation(Expose.class);

            Schema fieldSchema = generateFieldSchema(field, endpointType);
            properties.put(field.getName(), fieldSchema);
        }
        schema.setProperties(properties);
        return schema;
    }

    // Function to generate schema for a field
    private static Schema generateFieldSchema(Field field, EndpointType endpointType) {
        Class<?> fieldType = field.getType();
        fieldType = getFieldMappedType(fieldType);

        Schema fieldSchema;
        if (isSimpleType(fieldType)) {
            fieldSchema = generateSimpleFieldSchema(fieldType);
        } else if (fieldType.isArray()) {
            fieldSchema = generateArrayFieldSchema(field.getType().getComponentType(), endpointType);
        } else if (List.class.isAssignableFrom(fieldType)) {
            fieldSchema = generateListFieldSchema(field, endpointType);
        } else {
            fieldSchema = generateComplexFieldSchema(field.getType(), endpointType);
        }

        return fieldSchema;
    }

    private static Class<?> getFieldMappedType(Class<?> fieldType) {
        return typeMapping.getOrDefault(fieldType, fieldType);
    }

    private static Schema generateSimpleFieldSchema(Class<?> fieldType) {

        String fieldTypeName = FieldType.getByTypeName(fieldType.getSimpleName()).toString().toLowerCase();
        return mapJavaTypeToSwaggerType(fieldTypeName);
    }

    private static Schema mapJavaTypeToSwaggerType(String fieldTypeName) {
        Schema fieldSchema = new Schema();
        switch (fieldTypeName) {
            case "date_time":
            case "timestamp":
                fieldSchema.setType("string");
                fieldSchema.setFormat("date-time");
                break;
            case "long":
                fieldSchema.setType("integer");
                fieldSchema.setFormat("int64");
                break;
            case "float":
                fieldSchema.setType("number");
                fieldSchema.setFormat("float");
                break;
            case "double":
                fieldSchema.setType("number");
                fieldSchema.setFormat("double");
                break;
            default:
                fieldSchema.setType(fieldTypeName);
                break;
        }

        return fieldSchema;
    }

    private static Schema generateArrayFieldSchema(Class<?> componentType, EndpointType endpointType) {
        return new ArraySchema().items(generateSchema(componentType, endpointType));
    }

    private static Schema generateListFieldSchema(Field field, EndpointType endpointType) {
        ParameterizedType listType = (ParameterizedType) field.getGenericType();
        return new ArraySchema().items(generateSchema((Class<?>) listType.getActualTypeArguments()[0], endpointType));
    }

    private static Schema generateComplexFieldSchema(Class<?> fieldType, EndpointType endpointType) {
        return generateSchema(fieldType, endpointType);
    }


    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                Number.class.isAssignableFrom(clazz) ||
                clazz == Boolean.class ||
                clazz == Date.class ||
                clazz == java.sql.Date.class ||
                clazz == Timestamp.class;
    }


}
