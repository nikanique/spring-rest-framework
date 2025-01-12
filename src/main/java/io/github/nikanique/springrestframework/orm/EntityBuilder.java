package io.github.nikanique.springrestframework.orm;


import io.github.nikanique.springrestframework.annotation.Expose;
import io.github.nikanique.springrestframework.annotation.ReadOnly;
import io.github.nikanique.springrestframework.annotation.ReferencedModel;
import io.github.nikanique.springrestframework.dto.DtoManager;
import io.github.nikanique.springrestframework.dto.FieldMetadata;
import jakarta.persistence.EntityManager;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

public class EntityBuilder<Model> {

    private static final ConcurrentHashMap<Class<?>, EntityBuilder<?>> instances = new ConcurrentHashMap<>();
    private final Class<Model> entityClass;
    private final EntityManager entityManager;

    public EntityBuilder(Class<Model> entityClass, ApplicationContext springContext) {
        this.entityClass = entityClass;
        this.entityManager = springContext.getBean(EntityManager.class);
    }

    @SuppressWarnings("unchecked")
    public static <T> EntityBuilder<T> getInstance(Class<T> entityClass, ApplicationContext springContext) {
        return (EntityBuilder<T>) instances.computeIfAbsent(entityClass, k -> new EntityBuilder<>(entityClass, springContext));
    }

    public Model fromDto(Object dto, Class<?> dtoClass) {
        try {

            Map<String, FieldMetadata> fieldMetadata = DtoManager.getDtoByClassName(dtoClass);


            // Create an instance of the main entity class
            Model entity = this.entityClass.newInstance();
            BeanWrapper entityWrapper = new BeanWrapperImpl(entity);

            List<String> ignoreProperties = new ArrayList<>();
            for (String fieldName : fieldMetadata.keySet()) {
                Object fieldValue = fieldMetadata.get(fieldName).getGetterMethodHandle().invoke(dto);
                ReadOnly readOnlyAnnotation = fieldMetadata.get(fieldName).getReadOnly();
                if (readOnlyAnnotation != null && fieldValue == null) {
                    ignoreProperties.add(fieldName);
                    continue;
                }

                Expose exposeAnnotation = fieldMetadata.get(fieldName).getExpose();
                if (exposeAnnotation != null && !exposeAnnotation.source().equals("not-provided")) {
                    String sourceField = exposeAnnotation.source();
                    entityWrapper.setPropertyValue(sourceField, fieldValue);
                    ignoreProperties.add(sourceField);
                    continue;
                }


                // Check for the @ReferenceModel annotation
                if (fieldMetadata.get(fieldName).getReferencedModel() != null) {
                    ReferencedModel referenceModelAnnotation = fieldMetadata.get(fieldName).getReferencedModel();
                    String referencedModelName = referenceModelAnnotation.model();
                    String referencingFieldName = referenceModelAnnotation.referencingField();
                    Class<?> referencedModel = Class.forName(referencedModelName);
                    if (fieldValue != null) {
                        Object referencedEntity = entityManager.find(referencedModel, fieldValue);
                        if (referencedEntity == null) {
                            throw new IllegalArgumentException("Invalid ID for " + fieldName);
                        }
                        // Set the referenced entity to the corresponding field in the main entity
                        entityWrapper.setPropertyValue(referencingFieldName, referencedEntity);
                        ignoreProperties.add(fieldName);
                    }
                }
            }
            BeanUtils.copyProperties(dto, entity, ignoreProperties.toArray(new String[0]));

            return entity;
        } catch (InstantiationException | IllegalAccessException | ClassNotFoundException e) {
            throw new RuntimeException("Failed to instantiate entity class", e);
        } catch (Throwable e) {
            throw new RuntimeException(e);
        }
    }


}
