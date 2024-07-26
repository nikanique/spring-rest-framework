package io.github.nikanique.springrestframework.serializer;

import com.fasterxml.jackson.databind.node.ObjectNode;
import io.github.nikanique.springrestframework.annotation.Expose;
import io.github.nikanique.springrestframework.annotation.WriteOnly;
import io.github.nikanique.springrestframework.common.FieldType;
import lombok.extern.slf4j.Slf4j;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Field;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

@Slf4j
public class ClassStructureExtractor {

    private static final Set<Class<?>> visitedClasses = new HashSet<>();


    public static MethodHandle findToRepresentMethod(Class<?> clazz) {
        MethodType methodType = MethodType.methodType(ObjectNode.class, ObjectNode.class);
        MethodHandles.Lookup lookup = MethodHandles.lookup();
        try {
            return lookup.findStatic(clazz, "toRepresent", methodType);
        } catch (NoSuchMethodException e) {
            log.debug("toRepresent method is not find {}", e.getMessage());
        } catch (IllegalAccessException e) {
            log.error("Error finding toRepresent method: {}", e.getMessage());
        }

        return null;
    }


    public static HashMap<String, FieldDescriptor> extractStructure(Class<?> clazz) {
        HashMap<String, FieldDescriptor> structure = new HashMap<>();
        visitedClasses.clear(); // Clear the visited classes set
        extractFields(clazz, "", structure);
        return structure;
    }

    private static void extractFields(Class<?> clazz, String prefix, Map<String, FieldDescriptor> structure) {
        if (visitedClasses.contains(clazz)) {
            return; // Prevent revisiting the same class to avoid infinite recursion
        }
        visitedClasses.add(clazz);

        Field[] fields = clazz.getDeclaredFields();
        for (Field field : fields) {

            if (field.isAnnotationPresent(WriteOnly.class)) {
                continue;
            }

            Expose exposeAnnotation = field.getAnnotation(Expose.class);
            String exposeName = field.getName();
            String source = (exposeAnnotation != null && !exposeAnnotation.source().equals("not-provided")) ? exposeAnnotation.source() : field.getName();
            String format = (exposeAnnotation != null && !exposeAnnotation.format().equals("not-provided")) ? exposeAnnotation.format() : null;
            String methodName = (exposeAnnotation != null && !exposeAnnotation.methodName().equals("not-provided")) ? exposeAnnotation.methodName() : null;


            structure.put(prefix + exposeName, new FieldDescriptor(FieldType.getByTypeName(field.getType().getSimpleName()), exposeName, format, getMethodFullName(field, methodName, source), source));
            if (field.getType().getName().startsWith("java.util")) {
                String collectionType = field.getGenericType().getTypeName();
                if (collectionType.contains("<") && collectionType.contains(">")) {
                    String[] parts = collectionType.split("<");
                    String typeName = parts[1].substring(0, parts[1].length() - 1);
                    Class<?> innerClass;
                    try {
                        innerClass = Class.forName(typeName);
                        extractFields(innerClass, prefix + exposeName + "__", structure);
                    } catch (ClassNotFoundException e) {
                        log.error("Class not found: {}", typeName);
                    }
                }
            } else if (!field.getType().isPrimitive() && !isSimpleType(field.getType())) {
                extractFields(field.getType(), prefix + exposeName + "__", structure);
            }
        }
    }

    private static String getMethodFullName(Field field, String methodName, String source) {
        if (methodName == null || methodName.isEmpty()) {
            return null;
        }
        String declaringClassName = field.getDeclaringClass().getName();
        return declaringClassName + "." + methodName;
    }

    private static boolean isSimpleType(Class<?> clazz) {
        return clazz.isPrimitive() ||
                clazz == String.class ||
                clazz == Integer.class ||
                clazz == Long.class ||
                clazz == Double.class ||
                clazz == Float.class ||
                clazz == Boolean.class ||
                clazz == java.util.Date.class ||
                clazz == java.sql.Date.class ||
                clazz == java.sql.Timestamp.class;
    }

    private static String capitalize(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
}
