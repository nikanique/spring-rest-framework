package io.github.nikanique.springrestframework.configs;

import io.swagger.v3.oas.models.Operation;
import lombok.extern.slf4j.Slf4j;
import org.springdoc.core.customizers.OperationCustomizer;
import org.springframework.context.ApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.method.HandlerMethod;

import java.lang.reflect.Method;

@Configuration
@Slf4j
public class OpenAPIConfig {
    private final ApplicationContext applicationContext;

    public OpenAPIConfig(ApplicationContext applicationContext) {
        this.applicationContext = applicationContext;
    }

    @Bean
    public OperationCustomizer globalOperationCustomizer() {
        return (operation, handlerMethod) -> {
            Class<?> controllerClass = handlerMethod.getBeanType();
            try {
                // Look for the customizeOperationForController method in the controller class
                Method customizeMethod = controllerClass.getMethod("customizeOperationForController", Operation.class, HandlerMethod.class);
                // Make the method accessible if it's not public
                customizeMethod.setAccessible(true);
                Object controllerInstance = applicationContext.getBean(handlerMethod.getBean().toString());
                // Invoke the customizeOperationForController method
                customizeMethod.invoke(controllerInstance, operation, handlerMethod);
            } catch (NoSuchMethodException e) {
                log.debug("customizeOperationForController does not exist {}", e.getMessage());
            } catch (Exception e) {
                // Handle other potential exceptions
                log.error("Error occurred while invoking customizeOperationForController {}", e.getMessage());
            }
            return operation;
        };
    }
}
