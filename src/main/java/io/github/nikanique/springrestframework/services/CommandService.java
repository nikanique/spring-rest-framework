package io.github.nikanique.springrestframework.services;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.github.nikanique.springrestframework.annotation.Expose;
import io.github.nikanique.springrestframework.annotation.ReferencedModel;
import io.github.nikanique.springrestframework.dto.DtoManager;
import io.github.nikanique.springrestframework.dto.FieldMetadata;
import io.github.nikanique.springrestframework.exceptions.BadRequestException;
import jakarta.persistence.EntityManager;
import lombok.Getter;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import org.springframework.context.ApplicationContext;
import org.springframework.data.jpa.repository.JpaRepository;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.InvocationTargetException;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

@Getter
public class CommandService<EntityClass, ID> {
    private static final ConcurrentHashMap<Class<?>, CommandService<?, ?>> instances = new ConcurrentHashMap<>();

    private final EntityManager entityManager;
    private final ObjectMapper objectMapper;
    private final JpaRepository<EntityClass, ID> jpaRepository;

    public CommandService(JpaRepository<EntityClass, ID> jpaRepository, ApplicationContext springContext) {
        this.jpaRepository = jpaRepository;
        this.objectMapper = springContext.getBean(ObjectMapper.class);
        this.entityManager = springContext.getBean(EntityManager.class);
    }

    public static <EntityClass, ID> CommandService<EntityClass, ID> getInstance(
            Class<EntityClass> entityClass,
            JpaRepository<EntityClass, ID> jpaRepository, ApplicationContext springContext) {

        //noinspection unchecked
        return (CommandService<EntityClass, ID>) instances.computeIfAbsent(entityClass, k -> new CommandService<>(jpaRepository, springContext));
    }

    public EntityClass create(EntityClass entity) {
        entity = jpaRepository.save(entity);
        return entity;
    }


    public EntityClass update(EntityClass entityFromDB, Object dto, String lookupFieldName, Class<?> dtoClass) throws Throwable {
        BeanWrapper entityWrapper = new BeanWrapperImpl(entityFromDB);
        Map<String, FieldMetadata> fieldsMetadata = DtoManager.getDtoByClassName(dtoClass);
        for (Map.Entry<String, FieldMetadata> entry : fieldsMetadata.entrySet()) {
            if (entry.getValue().getGetterMethodHandle() == null) {
                continue;
            }

            String fieldName = entry.getKey();
            if (!fieldName.equals(lookupFieldName)) {
                try {
                    FieldMetadata fieldMetadata = entry.getValue();
                    Expose exposeAnnotation = fieldMetadata.getExpose();
                    ReferencedModel referenceModelAnnotation = fieldMetadata.getReferencedModel();
                    String sourceFieldName = fieldName;
                    if (exposeAnnotation != null && exposeAnnotation.source() != null) {
                        sourceFieldName = exposeAnnotation.source();
                    }
                    if (referenceModelAnnotation != null) {
                        sourceFieldName = referenceModelAnnotation.referencingField();
                        String referencedModelName = referenceModelAnnotation.model();
                        Class<?> referencedEntityClass = Class.forName(referencedModelName);
                        Object idValue = objectMapper.convertValue(entry.getValue().getGetterMethodHandle().invoke(dto), fieldMetadata.getFieldType());

                        if (idValue != null) {
                            Object referencedEntity = entityManager.find(referencedEntityClass, idValue);
                            if (referencedEntity == null) {
                                throw new IllegalArgumentException("Invalid ID for " + fieldName);
                            }
                            entityWrapper.setPropertyValue(sourceFieldName, referencedEntity);
                        }
                    } else {
                        setEntityFieldValue(entityFromDB, dto, entry, sourceFieldName);
                    }
                } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                         JsonProcessingException |
                         ClassNotFoundException e) {
                    throw new RuntimeException("Failed to update entity", e);
                }
            }
        }
        return jpaRepository.save(entityFromDB);
    }

    private void setEntityFieldValue(EntityClass entityFromDB, Object dto, Map.Entry<String, FieldMetadata> entry, String sourceFieldName) {
        try {
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            MethodHandle setterMethodHandle = lookup.findVirtual(entityFromDB.getClass(),
                    "set" + sourceFieldName.substring(0, 1).toUpperCase() + sourceFieldName.substring(1),
                    MethodType.methodType(void.class, entry.getValue().getFieldType()));
            Object value = objectMapper.convertValue(entry.getValue().getGetterMethodHandle().invoke(dto), entry.getValue().getFieldType());
            setterMethodHandle.invoke(entityFromDB, value);
        } catch (NoSuchMethodException e) {
            throw new BadRequestException(e.getMessage());
        } catch (Throwable e) {
            throw new RuntimeException(e.getMessage());
        }
    }


    public EntityClass update(EntityClass entityFromDB, Object dto, String lookupFieldName, Class<?> dtoClass, Set<String> fields) throws Throwable {
        BeanWrapper entityWrapper = new BeanWrapperImpl(entityFromDB);
        Map<String, FieldMetadata> fieldsMetadata = DtoManager.getDtoByClassName(dtoClass);
        for (Map.Entry<String, FieldMetadata> entry : fieldsMetadata.entrySet()) {
            if (entry.getValue().getGetterMethodHandle() == null || !fields.contains(entry.getKey()) || entry.getKey().equals(lookupFieldName)) {
                continue;
            }
            try {
                String fieldName = entry.getKey();
                FieldMetadata fieldMetadata = entry.getValue();
                Expose exposeAnnotation = fieldMetadata.getExpose();
                ReferencedModel referenceModelAnnotation = fieldMetadata.getReferencedModel();
                String sourceFieldName = fieldName;
                if (exposeAnnotation != null && exposeAnnotation.source() != null) {
                    sourceFieldName = exposeAnnotation.source();
                }
                if (referenceModelAnnotation != null) {
                    sourceFieldName = referenceModelAnnotation.referencingField();
                    String referencedModelName = referenceModelAnnotation.model();
                    Class<?> referencedEntityClass = Class.forName(referencedModelName);
                    Object idValue = objectMapper.convertValue(entry.getValue().getGetterMethodHandle().invoke(dto), fieldMetadata.getFieldType());

                    if (idValue != null) {
                        Object referencedEntity = entityManager.find(referencedEntityClass, idValue);
                        if (referencedEntity == null) {
                            throw new IllegalArgumentException("Invalid ID for " + fieldName);
                        }
                        entityWrapper.setPropertyValue(sourceFieldName, referencedEntity);
                    }
                } else {
                    setEntityFieldValue(entityFromDB, dto, entry, sourceFieldName);
                }
            } catch (NoSuchMethodException | IllegalAccessException | InvocationTargetException |
                     JsonProcessingException |
                     ClassNotFoundException e) {
                throw new RuntimeException("Failed to update entity", e);
            }

        }
        return jpaRepository.save(entityFromDB);
    }

    public void delete(EntityClass entity) {
        jpaRepository.delete(entity);
    }

}
