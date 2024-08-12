package io.github.nikanique.springrestframework.serializer;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ArrayNode;
import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.common.FieldType;
import io.github.nikanique.springrestframework.utilities.MethodReflectionHelper;
import io.github.nikanique.springrestframework.utilities.StringUtils;
import io.github.nikanique.springrestframework.utilities.ValueFormatter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Method;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.*;

@Component
@Slf4j
public class Serializer {

    private final ObjectMapper objectMapper;

    @Autowired
    public Serializer(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public Object deserialize(String requestBody, Class<?> dtoClass) throws IOException {
        return deserialize(requestBody, dtoClass, false, false);
    }

    public Object deserialize(String requestBody, Class<?> dtoClass, Boolean raiseValidationError) throws IOException {
        return deserialize(requestBody, dtoClass, raiseValidationError, false);
    }

    public Object deserialize(String requestBody, Class<?> dtoClass, Boolean raiseValidationError, Boolean partial) throws IOException {
        Set<String> fieldNames = new HashSet<>();
        if (partial) {
            fieldNames = getPresentFields(requestBody);
        }
        return deserialize(requestBody, dtoClass, raiseValidationError, fieldNames);

    }

    public Object deserialize(String requestBody, Class<?> dtoClass, Boolean raiseValidationError, Set<String> fields) throws IOException {
        Object dto = objectMapper.readValue(requestBody, dtoClass);
        if (!fields.isEmpty()) {
            invokeValidateIfExists(dto, dtoClass, raiseValidationError, fields);
        } else {
            invokeValidateIfExists(dto, dtoClass, raiseValidationError);
        }

        return dtoClass.cast(dto);
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
