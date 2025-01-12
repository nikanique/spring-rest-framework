package io.github.nikanique.springrestframework.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.annotation.Expose;
import io.github.nikanique.springrestframework.annotation.ReadOnly;
import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.dto.DtoManager;
import io.github.nikanique.springrestframework.dto.FieldMetadata;
import io.github.nikanique.springrestframework.exceptions.BadRequestException;
import io.github.nikanique.springrestframework.utilities.MethodReflectionHelper;
import io.github.nikanique.springrestframework.utilities.StringUtils;
import io.github.nikanique.springrestframework.utilities.ValueFormatter;
import jakarta.persistence.EntityManager;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.sql.Timestamp;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@Slf4j
public class Serializer {

    private final ObjectMapper objectMapper;
    private final EntityManager entityManager;

    @Autowired
    public Serializer(ObjectMapper objectMapper, EntityManager entityManager) {
        this.objectMapper = objectMapper;
        this.entityManager = entityManager;
    }

    public Object deserialize(String requestBody, Class<?> dtoClass) throws Throwable {
        return deserialize(requestBody, dtoClass, false, false);
    }

    public Object deserialize(String requestBody, Class<?> dtoClass, Boolean raiseValidationError) throws Throwable {
        return deserialize(requestBody, dtoClass, raiseValidationError, false);
    }

    public Object deserialize(String requestBody, Class<?> dtoClass, Boolean raiseValidationError, Boolean partial) throws Throwable {
        Set<String> fieldNames = new HashSet<>();
        if (partial) {
            fieldNames = getPresentFields(requestBody);
        }
        return deserialize(requestBody, dtoClass, raiseValidationError, fieldNames);

    }

    public Object deserialize(String requestBody, Class<?> dtoClass, Boolean raiseValidationError, Set<String> fields) throws Throwable {
        Object dto = generateDTO(requestBody, dtoClass);
        if (!fields.isEmpty()) {
            invokeValidateIfExists(dto, dtoClass, raiseValidationError, fields);
        } else {
            invokeValidateIfExists(dto, dtoClass, raiseValidationError);
        }
        invokePostDeserialization(dto, dtoClass, entityManager);
        return dtoClass.cast(dto);
    }

    private Object generateDTO(String requestBody, Class<?> dtoClass) throws Throwable {
        Object dto = objectMapper.readValue(requestBody, dtoClass);
        Map<String, FieldMetadata> fieldMetadata = DtoManager.getDtoByClassName(dtoClass);
        for (String fieldName : fieldMetadata.keySet()) {
            ReadOnly readOnlyAnnotation = fieldMetadata.get(fieldName).getReadOnly();
            Expose exposeAnnotation = fieldMetadata.get(fieldName).getExpose();
            Object fieldValue = fieldMetadata.get(fieldName).getGetterMethodHandle().invoke(dto);

            // Read only field set to null
            if (readOnlyAnnotation != null && fieldValue != null) {
                try {
                    fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, null);
                } catch (Throwable e) {
                    log.error("{} setter method invocation failed.", fieldName);
                }
            }


            // Default value
            if (readOnlyAnnotation == null && exposeAnnotation != null && fieldValue == null &&
                    !exposeAnnotation.defaultValue().equals("not-provided")) {
                Class<?> fieldType = fieldMetadata.get(fieldName).getFieldType();
                String defaultValue = exposeAnnotation.defaultValue();
                try {
                    if (fieldType == Integer.class || fieldType == int.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, Integer.valueOf(defaultValue));
                    } else if (fieldType == Long.class || fieldType == long.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, Long.valueOf(defaultValue));
                    } else if (fieldType == Double.class || fieldType == double.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, Double.valueOf(defaultValue));
                    } else if (fieldType == Float.class || fieldType == float.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, Float.valueOf(defaultValue));
                    } else if (fieldType == LocalDate.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, LocalDate.parse(defaultValue));
                    } else if (fieldType == Date.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, java.sql.Date.valueOf(defaultValue));
                    } else if (fieldType == Timestamp.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, Timestamp.valueOf(defaultValue));
                    } else if (fieldType == String.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, defaultValue);
                    } else if (fieldType == Boolean.class || fieldType == boolean.class) {
                        fieldMetadata.get(fieldName).getSetterMethodHandle().invoke(dto, Boolean.valueOf(defaultValue));
                    } else {
                        log.error("{} field type is not supported for default value.", fieldName);
                    }
                } catch (Throwable e) {
                    log.error("{} default value could not be set.", fieldName);
                }
            }

            // Required check

            if (exposeAnnotation != null && fieldValue == null && exposeAnnotation.isRequired()) {
                throw new BadRequestException(fieldName, "Field is required.");
            }
        }
        return dto;
    }

    public Set<String> getPresentFields(String requestBody) throws JsonProcessingException {
        JsonNode requestBodyNode = objectMapper.readTree(requestBody);
        Set<String> fieldNames = new HashSet<>();
        requestBodyNode.fieldNames().forEachRemaining(fieldNames::add);
        return fieldNames;
    }

    private void invokeValidateIfExists(Object dto, Class<?> dtoClass, Boolean raiseValidationError) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType methodType = MethodType.methodType(Map.class, Boolean.class);
        try {
            MethodHandle validateMethodHandle = lookup.findVirtual(dtoClass, "validate", methodType);
            validateMethodHandle.invoke(dto, raiseValidationError);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // No validate method exists, or it is not accessible, do nothing
        } catch (Throwable t) {
            // Handle other possible exceptions from MethodHandle.invoke
            throw new RuntimeException("Validation failed", t);
        }
    }


    private void invokeValidateIfExists(Object dto, Class<?> dtoClass, Boolean raiseValidationError, Set<String> fieldNames) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType methodType = MethodType.methodType(Map.class, Set.class, Boolean.class);
        try {
            MethodHandle validateMethodHandle = lookup.findVirtual(dtoClass, "validate", methodType);
            validateMethodHandle.invoke(dto, fieldNames, raiseValidationError);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            // No validate method exists, or it is not accessible, do nothing
        } catch (Throwable t) {
            // Handle other possible exceptions from MethodHandle.invoke
            throw new RuntimeException("Validation failed", t);
        }
    }

    private void invokePostDeserialization(Object dto, Class<?> dtoClass, EntityManager entityManager) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        MethodType methodType = MethodType.methodType(void.class, EntityManager.class);
        try {
            MethodHandle validateMethodHandle = lookup.findVirtual(dtoClass, "postDeserialization", methodType);
            validateMethodHandle.invoke(dto, entityManager);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error("postDeserialization method does not exist or is not accessible");
        } catch (Throwable t) {
            throw new RuntimeException("postDeserialization method failed", t);
        }
    }


    public ObjectNode serialize(Object object, SerializerConfig serializerConfig) {
        ObjectNode serializedData = serializeObject(object, serializerConfig.getFields(), "");
        if (serializerConfig.getToRepresentMethod() != null) {
            try {
                serializedData = (ObjectNode) serializerConfig.getToRepresentMethod().invoke(serializedData);
            } catch (Throwable e) {
                throw new RuntimeException(e);
            }
        }

        return serializedData;
    }

    private ObjectNode serializeObject(Object object, HashMap<String, FieldDescriptor> fields, String parentPrefix) {
        ObjectNode objectNode = objectMapper.createObjectNode();
        int numberOfParentPrefixes = StringUtils.countOfOccurrences(parentPrefix, "__");
        for (String fieldName : fields.keySet()) {
            if (!fieldName.startsWith(parentPrefix) ||
                    (numberOfParentPrefixes < StringUtils.countOfOccurrences(fieldName, "__"))
            ) {
                continue;
            }

            String fieldSuffix = fieldName.substring(parentPrefix.length());
            if (fieldSuffix.contains("__")) {
                fieldSuffix = fieldSuffix.substring(0, fieldSuffix.indexOf("__"));
            }

            FieldDescriptor fieldDescriptor = fields.get(fieldName);
            String exposeName = fieldDescriptor.getExposeName();
            FieldType fieldType = fieldDescriptor.getFieldType();
            String valueFormat = fieldDescriptor.getFormat();
            String methodName = fieldDescriptor.getMethodName();
            String source = fieldDescriptor.getSource();

            try {
                Object fieldValue = getNestedFieldValue(object, source);
                if (fieldValue != null) {
                    if (fieldValue instanceof Collection) {
                        ArrayNode arrayNode = serializeCollection((Collection<?>) fieldValue, fields, parentPrefix + fieldSuffix + "__");
                        objectNode.set(exposeName == null ? fieldSuffix : exposeName, arrayNode);
                    } else if (fieldValue.getClass().isArray()) {
                        ArrayNode arrayNode = serializeArray(fieldValue, fields, parentPrefix + fieldSuffix + "__");
                        objectNode.set(exposeName == null ? fieldSuffix : exposeName, arrayNode);
                    } else if (isSimpleType(fieldValue.getClass())) {
                        if (methodName != null) {
                            fieldValue = MethodReflectionHelper.invokeMethodFromString(methodName, fieldValue);
                        }
                        objectNode.putPOJO(exposeName == null ? fieldSuffix : exposeName,
                                ValueFormatter.formatValue(fieldValue, fieldType, valueFormat));
                    } else {
                        ObjectNode nestedObjectNode = serializeObject(fieldValue, fields, parentPrefix + fieldSuffix + "__");
                        objectNode.set(exposeName == null ? fieldSuffix : exposeName, nestedObjectNode);
                    }
                } else {
                    objectNode.putPOJO(exposeName == null ? fieldSuffix : exposeName, null);
                }
            } catch (Throwable throwable) {
                log.error(throwable.getMessage());
            }
        }

        return objectNode;
    }

    private Object getNestedFieldValue(Object object, String fieldPath) throws Throwable {
        String[] fieldNames = fieldPath.split("__");
        Object currentObject = object;
        for (String fieldName : fieldNames) {
            if (currentObject == null) {
                return null;
            }
            MethodHandle getter = findGetter(currentObject.getClass(), fieldName);
            if (getter == null) {
                return null;
            }
            currentObject = getter.invoke(currentObject);
        }
        return currentObject;
    }

    private MethodHandle findGetter(Class<?> clazz, String fieldName) {
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            Method method = clazz.getMethod("get" + capitalize(fieldName));
            return lookup.unreflect(method);
        } catch (NoSuchMethodException | IllegalAccessException e) {
            log.error(e.getMessage());
            return null;
        }
    }

    private ArrayNode serializeCollection(Collection<?> collection, HashMap<String, FieldDescriptor> fields, String parentPrefix) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        for (Object item : collection) {
            if (item != null) {
                if (isSimpleType(item.getClass())) {
                    arrayNode.addPOJO(item);
                } else {
                    ObjectNode nestedObjectNode = serializeObject(item, fields, parentPrefix);
                    arrayNode.add(nestedObjectNode);
                }
            }
        }
        return arrayNode;
    }

    private ArrayNode serializeArray(Object array, HashMap<String, FieldDescriptor> fields, String parentPrefix) {
        ArrayNode arrayNode = objectMapper.createArrayNode();
        int length = java.lang.reflect.Array.getLength(array);
        for (int i = 0; i < length; i++) {
            Object item = java.lang.reflect.Array.get(array, i);
            if (item != null) {
                if (isSimpleType(item.getClass())) {
                    arrayNode.addPOJO(item);
                } else {
                    ObjectNode nestedObjectNode = serializeObject(item, fields, parentPrefix);
                    arrayNode.add(nestedObjectNode);
                }
            }
        }
        return arrayNode;
    }

    private boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Double.class ||
                clazz == Float.class ||
                clazz == Boolean.class ||
                clazz == Date.class ||
                clazz == java.sql.Date.class ||
                clazz == java.sql.Timestamp.class ||
                clazz == LocalDateTime.class ||
                clazz == LocalDate.class ||
                clazz == LocalTime.class;

    }

    private String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
