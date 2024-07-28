package io.github.nikanique.springrestframework.orm;

import io.github.nikanique.springrestframework.filter.FilterOperation;
import jakarta.persistence.EntityManagerFactory;
import jakarta.persistence.criteria.*;
import jakarta.persistence.metamodel.Attribute;
import jakarta.persistence.metamodel.EntityType;
import jakarta.persistence.metamodel.Metamodel;
import lombok.AllArgsConstructor;
import org.springframework.data.jpa.domain.Specification;

import java.sql.Timestamp;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@SuppressWarnings({"unchecked", "rawtypes"})
@AllArgsConstructor
public class ModelSpecification<Model> implements Specification<Model> {

    private SearchCriteria searchCriteria;
    private EntityManagerFactory entityManagerFactory;

    @Override
    public Predicate toPredicate(Root<Model> root, CriteriaQuery<?> query, CriteriaBuilder builder) {
        Path<?> path = getPath(root, searchCriteria.getKey());
        Class<?> attributeType = path.getJavaType();
        Object value = searchCriteria.getValue();

        if (searchCriteria.getFilterOperation() != FilterOperation.IN) {
            if (attributeType == Integer.class || attributeType == int.class) {
                value = Integer.valueOf(value.toString());
            } else if (attributeType == Long.class || attributeType == long.class) {
                value = Long.valueOf(value.toString());
            } else if (attributeType == Double.class || attributeType == double.class) {
                value = Double.valueOf(value.toString());
            } else if (attributeType == Float.class || attributeType == float.class) {
                value = Float.valueOf(value.toString());
            } else if (attributeType == LocalDate.class) {
                value = LocalDate.parse(value.toString());
            } else if (attributeType == Date.class) {
                value = java.sql.Date.valueOf(value.toString());
            } else if (attributeType == Timestamp.class) {
                value = Timestamp.valueOf(value.toString());
            }
        }
        switch (searchCriteria.getFilterOperation()) {
            case GREATER:
                return createGreaterThanPredicate(builder, path, attributeType, value);
            case GREATER_OR_EQUAL:
                return createGreaterThanOrEqualToPredicate(builder, path, attributeType, value);
            case LESS:
                return createLessThanPredicate(builder, path, attributeType, value);
            case LESS_OR_EQUAL:
                return createLessThanOrEqualToPredicate(builder, path, attributeType, value);
            case EQUAL:
                return builder.equal(path, value);
            case CONTAINS:
                if (attributeType == String.class) {
                    return builder.like(path.as(String.class), "%" + value + "%");
                }
                break;
            case IN:
                return createInPredicate(builder, path, attributeType, value.toString());
            default:
                return null;
        }
        return null;
    }

    private Path<?> getPath(Root<?> root, String key) {
        String[] keys = key.split("__");
        Path<?> path = root;
        for (String k : keys) {
            if (isAssociation(path, k)) {
                path = ((From<?, ?>) path).join(k, JoinType.LEFT);
            } else {
                path = path.get(k);
            }
        }
        return path;
    }

    private boolean isAssociation(Path<?> path, String attributeName) {
        Metamodel metamodel = entityManagerFactory.getMetamodel();
        EntityType<?> entityType = metamodel.entity(path.getJavaType());
        Attribute<?, ?> attribute = entityType.getAttribute(attributeName);
        return attribute.isAssociation();
    }

    private Predicate createGreaterThanPredicate(CriteriaBuilder builder, Path<?> path, Class<?> attributeType, Object value) {
        if (attributeType == Integer.class || attributeType == int.class) {
            return builder.greaterThan(path.as(Integer.class), (Integer) value);
        } else if (attributeType == Long.class || attributeType == long.class) {
            return builder.greaterThan(path.as(Long.class), (Long) value);
        } else if (attributeType == Double.class || attributeType == double.class) {
            return builder.greaterThan(path.as(Double.class), (Double) value);
        } else if (attributeType == Float.class || attributeType == float.class) {
            return builder.greaterThan(path.as(Float.class), (Float) value);
        } else if (attributeType == Date.class) {
            return builder.greaterThan(path.as(Date.class), (Date) value);
        } else if (attributeType == LocalDate.class) {
            return builder.greaterThan(path.as(LocalDate.class), (LocalDate) value);
        } else if (attributeType == Timestamp.class) {
            return builder.greaterThan(path.as(Timestamp.class), (Timestamp) value);
        } else if (Comparable.class.isAssignableFrom(attributeType))
            return builder.greaterThan(path.as(Comparable.class), (Comparable) value);
        return null;
    }

    private Predicate createGreaterThanOrEqualToPredicate(CriteriaBuilder builder, Path<?> path, Class<?> attributeType, Object value) {
        if (attributeType == Integer.class || attributeType == int.class) {
            return builder.greaterThanOrEqualTo(path.as(Integer.class), (Integer) value);
        } else if (attributeType == Long.class || attributeType == long.class) {
            return builder.greaterThanOrEqualTo(path.as(Long.class), (Long) value);
        } else if (attributeType == Double.class || attributeType == double.class) {
            return builder.greaterThanOrEqualTo(path.as(Double.class), (Double) value);
        } else if (attributeType == Float.class || attributeType == float.class) {
            return builder.greaterThanOrEqualTo(path.as(Float.class), (Float) value);
        } else if (attributeType == Date.class) {
            return builder.greaterThanOrEqualTo(path.as(Date.class), (Date) value);
        } else if (attributeType == LocalDate.class) {
            return builder.greaterThanOrEqualTo(path.as(LocalDate.class), (LocalDate) value);
        } else if (attributeType == Timestamp.class) {
            return builder.greaterThanOrEqualTo(path.as(Timestamp.class), (Timestamp) value);
        } else if (Comparable.class.isAssignableFrom(attributeType)) {
            return builder.greaterThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
        }
        return null;
    }

    private Predicate createLessThanPredicate(CriteriaBuilder builder, Path<?> path, Class<?> attributeType, Object value) {
        if (attributeType == Integer.class || attributeType == int.class) {
            return builder.lessThan(path.as(Integer.class), (Integer) value);
        } else if (attributeType == Long.class || attributeType == long.class) {
            return builder.lessThan(path.as(Long.class), (Long) value);
        } else if (attributeType == Double.class || attributeType == double.class) {
            return builder.lessThan(path.as(Double.class), (Double) value);
        } else if (attributeType == Float.class || attributeType == float.class) {
            return builder.lessThan(path.as(Float.class), (Float) value);
        } else if (attributeType == Date.class) {
            return builder.lessThan(path.as(Date.class), (Date) value);
        } else if (attributeType == LocalDate.class) {
            return builder.lessThan(path.as(LocalDate.class), (LocalDate) value);
        } else if (attributeType == Timestamp.class) {
            return builder.lessThan(path.as(Timestamp.class), (Timestamp) value);
        } else if (Comparable.class.isAssignableFrom(attributeType)) {
            return builder.lessThan(path.as(Comparable.class), (Comparable) value);
        }
        return null;
    }

    private Predicate createLessThanOrEqualToPredicate(CriteriaBuilder builder, Path<?> path, Class<?> attributeType, Object value) {
        if (attributeType == Integer.class || attributeType == int.class) {
            return builder.lessThanOrEqualTo(path.as(Integer.class), (Integer) value);
        } else if (attributeType == Long.class || attributeType == long.class) {
            return builder.lessThanOrEqualTo(path.as(Long.class), (Long) value);
        } else if (attributeType == Double.class || attributeType == double.class) {
            return builder.lessThanOrEqualTo(path.as(Double.class), (Double) value);
        } else if (attributeType == Float.class || attributeType == float.class) {
            return builder.lessThanOrEqualTo(path.as(Float.class), (Float) value);
        } else if (attributeType == Date.class) {
            return builder.lessThanOrEqualTo(path.as(Date.class), (Date) value);
        } else if (attributeType == LocalDate.class) {
            return builder.lessThanOrEqualTo(path.as(LocalDate.class), (LocalDate) value);
        } else if (attributeType == Timestamp.class) {
            return builder.lessThanOrEqualTo(path.as(Timestamp.class), (Timestamp) value);
        } else if (Comparable.class.isAssignableFrom(attributeType)) {
            return builder.lessThanOrEqualTo(path.as(Comparable.class), (Comparable) value);
        }
        return null;
    }

    private Predicate createInPredicate(CriteriaBuilder builder, Path<?> path, Class<?> attributeType, String value) {
        List<?> values = Arrays.stream(value.split(","))
                .map(String::trim)
                .map(v -> castToAttributeType(v, attributeType))
                .collect(Collectors.toList());

        CriteriaBuilder.In<Object> inClause = builder.in(path);
        for (Object val : values) {
            inClause.value(val);
        }
        return inClause;
    }

    private Object castToAttributeType(String value, Class<?> attributeType) {
        if (attributeType == Integer.class || attributeType == int.class) {
            return Integer.parseInt(value);
        } else if (attributeType == Long.class || attributeType == long.class) {
            return Long.parseLong(value);
        } else if (attributeType == Double.class || attributeType == double.class) {
            return Double.parseDouble(value);
        } else if (attributeType == Float.class || attributeType == float.class) {
            return Float.parseFloat(value);
        } else if (attributeType == LocalDate.class) {
            return LocalDate.parse(value);
        } else if (attributeType == Timestamp.class) {
            return Timestamp.valueOf(value);
        } else if (attributeType == Date.class) {
            return java.sql.Date.valueOf(value);
        } else if (attributeType == String.class) {
            return value;
        }

        throw new IllegalArgumentException("Unsupported attribute type: " + attributeType);
    }
}
