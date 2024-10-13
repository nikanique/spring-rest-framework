package io.github.nikanique.springrestframework.utilities;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.reflect.Method;

public class MethodReflectionHelper {


    public static Object invokeMethodFromString(String fullMethodName, Object argument) {
        int lastDotIndex = fullMethodName.lastIndexOf(".");
        if (lastDotIndex == -1 || lastDotIndex == fullMethodName.length() - 1) {
            throw new IllegalArgumentException("Invalid full method name");
        }
        String className = fullMethodName.substring(0, lastDotIndex);
        String methodName = fullMethodName.substring(lastDotIndex + 1);

        Class<?> methodClass;
        try {
            methodClass = Class.forName(className);
            Method method = methodClass.getMethod(methodName, Object.class);
            return method.invoke(null, argument);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }


    public static Method findRepositoryMethod(String methodName, JpaRepository repository) throws NoSuchMethodException {
        // Iterate through all methods in the repository
        for (Method method : repository.getClass().getMethods()) {
            // Check if the method name matches
            if (method.getName().equals(methodName)) {
                // Check parameter types: should be Specification<Model> and Pageable
                Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 2 &&
                        Specification.class.isAssignableFrom(parameterTypes[0]) &&
                        Pageable.class.isAssignableFrom(parameterTypes[1])) {

                    // Check return type: should be Model or Page<Model>
                    Class<?> returnType = method.getReturnType();
                    if (Page.class.isAssignableFrom(returnType)) {
                        return method;
                    }
                }
            }
        }

        throw new NoSuchMethodException("No matching method found with name: " + methodName);
    }
}
