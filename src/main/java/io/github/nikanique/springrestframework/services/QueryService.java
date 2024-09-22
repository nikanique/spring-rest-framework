package io.github.nikanique.springrestframework.services;


import io.github.nikanique.springrestframework.annotation.SrfQuery;
import io.github.nikanique.springrestframework.orm.SearchCriteria;
import io.github.nikanique.springrestframework.orm.SpecificationsBuilder;
import io.github.nikanique.springrestframework.utilities.StringUtils;
import lombok.extern.slf4j.Slf4j;
import net.bytebuddy.ByteBuddy;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;
import net.bytebuddy.implementation.FieldAccessor;
import org.springframework.context.ApplicationContext;
import org.springframework.core.annotation.AnnotationUtils;
import org.springframework.data.domain.*;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.ResultSetExtractor;

import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;


@SuppressWarnings({"unchecked", "rawtypes"})
@Slf4j
public class QueryService<Model> {

    private static final ConcurrentHashMap<Class<?>, QueryService<?>> instances = new ConcurrentHashMap<>();
    private final JpaSpecificationExecutor<Model> jpaSpecificationExecutor;
    private final SpecificationsBuilder specificationsBuilder;
    private final Map<String, Class<?>> classCache = new HashMap<>();
    private final JdbcTemplate jdbcTemplate;
    private final Map<Method, String> methodQueries = new HashMap<>();
    private final Map<Method, MethodHandle> methodHandles = new HashMap<>();

    private QueryService(JpaSpecificationExecutor<Model> jpaSpecificationExecutor, ApplicationContext springContext) {
        this.jpaSpecificationExecutor = jpaSpecificationExecutor;
        this.specificationsBuilder = springContext.getBean(SpecificationsBuilder.class);
        this.jdbcTemplate = springContext.getBean(JdbcTemplate.class);

    }

    public static <Model> QueryService<Model> getInstance(
            Class<Model> entityClass,
            JpaSpecificationExecutor<Model> jpaSpecificationExecutor,
            ApplicationContext springContext) {

        return (QueryService<Model>) instances.computeIfAbsent(entityClass, k -> new QueryService<>(jpaSpecificationExecutor, springContext));
    }

    public Optional<Object> getObject(List<SearchCriteria> searchCriteriaList, Method queryMethod) throws Throwable {
        Specification specifications = this.specificationsBuilder.fromSearchCriteriaList(searchCriteriaList);
        if (getSqlQuery(queryMethod) != null) {
            return Optional.of(executeQueryForObject(getSqlQuery(queryMethod), searchCriteriaList));
        }
        return invokeQueryMethodForOneObject(queryMethod, specifications);
    }

    private Optional<Object> invokeQueryMethodForOneObject(Method queryMethod, Specification specifications) throws Throwable {
        Pageable pageable = PageRequest.of(0, 1);
        Page<Object> results = (Page<Object>) getMethodHandle(queryMethod).invoke(jpaSpecificationExecutor, specifications, pageable);
        if (results.getTotalElements() > 1) {
            throw new IllegalStateException("More than one result found");
        }
        return results.get().findFirst();
    }

    public Optional<Object> getObject(List<SearchCriteria> searchCriteriaList) {
        Specification specifications = this.specificationsBuilder.fromSearchCriteriaList(searchCriteriaList);
        return jpaSpecificationExecutor.findOne(specifications);
    }

    public Page<Object> getPagedlist(List<SearchCriteria> searchCriteriaList, int page, int size, Sort.Direction direction, String sortBy, Method queryMethod) throws Throwable {
        Specification specifications = this.specificationsBuilder.fromSearchCriteriaList(searchCriteriaList);
        Pageable pageable;
        if (sortBy.isEmpty()) {
            pageable = PageRequest.of(page, size, Sort.unsorted());
        } else {
            pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        }
        if (getSqlQuery(queryMethod) != null) {
            return executeQueryForPagedList(getSqlQuery(queryMethod), searchCriteriaList, pageable);
        }
        return (Page<Object>) getMethodHandle(queryMethod).invoke(jpaSpecificationExecutor, specifications, pageable);
    }

    public Page<Object> getPagedlist(List<SearchCriteria> searchCriteriaList, int page, int size, Sort.Direction direction, String sortBy) throws Throwable {
        Specification specifications = this.specificationsBuilder.fromSearchCriteriaList(searchCriteriaList);
        Pageable pageable;
        if (sortBy.isEmpty()) {
            pageable = PageRequest.of(page, size, Sort.unsorted());
        } else {
            pageable = PageRequest.of(page, size, Sort.by(direction, sortBy));
        }
        return (Page<Object>) jpaSpecificationExecutor.findAll(specifications, pageable);
    }

    public Object executeQueryForObject(String sqlQuery, List<SearchCriteria> searchCriteriaList) {
        String whereClause = SearchCriteria.generateSqlWhereClause(searchCriteriaList);

        String finalQuery = sqlQuery.replace("${whereClause}", whereClause)
                .replace("${pagination}", "");

        // Execute the query and create dynamic row objects
        List<Object> results = jdbcTemplate.query(finalQuery, new ResultSetExtractor<List<Object>>() {
            @Override
            public List<Object> extractData(ResultSet resultSet) throws SQLException {
                List<Object> resultList = new ArrayList<>();
                // Create dynamic class with fields for each column
                Class<?> rowClass = getOrCreateDynamicClass(resultSet, sqlQuery);
                while (resultSet.next()) {
                    Object rowObject = createRowObject(resultSet, rowClass);
                    resultList.add(rowObject);
                }

                return resultList;
            }
        });

        if (results.isEmpty()) {
            return null;
        }
        if (results.size() > 1) {
            throw new IllegalStateException("More than one result found");
        }
        return results.get(0);
    }


    public Page<Object> executeQueryForPagedList(String sqlQuery, List<SearchCriteria> searchCriteriaList, Pageable pageable) {
        String whereClause = SearchCriteria.generateSqlWhereClause(searchCriteriaList);
        String paginationClause = buildPaginationClause(pageable);

        String finalQuery = sqlQuery.replace("${whereClause}", whereClause)
                .replace("${pagination}", paginationClause);

        // Execute the query and create dynamic row objects
        List<Object> results = jdbcTemplate.query(finalQuery, new ResultSetExtractor<List<Object>>() {
            @Override
            public List<Object> extractData(ResultSet resultSet) throws SQLException {
                List<Object> resultList = new ArrayList<>();
                // Create dynamic class with fields for each column
                Class<?> rowClass = getOrCreateDynamicClass(resultSet, sqlQuery);
                while (resultSet.next()) {
                    Object rowObject = createRowObject(resultSet, rowClass);
                    resultList.add(rowObject);
                }

                return resultList;
            }
        });

        // Get total count for pagination
        String countQuery = "SELECT COUNT(*) FROM (" + sqlQuery.replace("${whereClause}", whereClause).replace("${pagination}", "") + ") AS count_query";
        int total = jdbcTemplate.queryForObject(countQuery, Integer.class);

        // Create a Page object
        return new PageImpl<>(results, pageable, total);
    }


    private Class<?> getOrCreateDynamicClass(ResultSet rs, String query) {
        // Create a cache key based on the query
        String cacheKey = StringUtils.generateHashCode(query);
        return classCache.computeIfAbsent(cacheKey, k -> {
            try {
                return createDynamicClass(rs);
            } catch (SQLException e) {
                throw new RuntimeException(e);
            }
        });
    }


    private Class<?> createDynamicClass(ResultSet rs) throws SQLException {

        ByteBuddy byteBuddy = new ByteBuddy();
        net.bytebuddy.dynamic.DynamicType.Builder<?> builder = byteBuddy.subclass(Object.class).name("DynamicRow");
        int columnCount = rs.getMetaData().getColumnCount();
        for (int i = 1; i <= columnCount; i++) {
            String columnName = rs.getMetaData().getColumnLabel(i);
            builder = builder.defineField(columnName, Object.class)
                    .defineMethod("get" + StringUtils.capitalize(columnName), Object.class, Modifier.PUBLIC)
                    .intercept(FieldAccessor.ofField(columnName));
        }
        return builder.make().load(getClass().getClassLoader(), ClassLoadingStrategy.Default.CHILD_FIRST).getLoaded();
    }


    private Object createRowObject(ResultSet rs, Class<?> rowClass) throws SQLException {
        try {
            Object rowObject = rowClass.getDeclaredConstructor().newInstance();
            int columnCount = rs.getMetaData().getColumnCount();
            for (int i = 1; i <= columnCount; i++) {
                String columnName = rs.getMetaData().getColumnLabel(i);
                Object value = rs.getObject(i);
                Field field = rowClass.getDeclaredField(columnName);
                field.setAccessible(true);
                field.set(rowObject, value);
            }
            return rowObject;
        } catch (Exception e) {
            throw new SQLException("Failed to create row object", e);
        }
    }


    private String buildPaginationClause(Pageable pageable) {
        if (pageable == null) {
            return "";
        }
        String sortColumn = pageable.getSort().toString().replace(":", " ");
        int pageNumber = pageable.getPageNumber();
        int pageSize = pageable.getPageSize();
        int offset = pageNumber * pageSize;
        String orderBy = sortColumn.equals("UNSORTED") ? "" : " ORDER BY " + sortColumn;
        return orderBy + " " + "LIMIT " + pageSize + " OFFSET " + offset;
    }


    private String getSqlQuery(Method method) {
        return methodQueries.computeIfAbsent(method, queryMethod -> {
            // Use AnnotationUtils to find the @SrfQuery annotation on the method
            SrfQuery queryAnnotation = AnnotationUtils.findAnnotation(method, SrfQuery.class);
            if (queryAnnotation != null && !queryAnnotation.value().equals("")) {
                // Return the query value as a string
                return queryAnnotation.value();
            }
            return null;
        });
    }

    private MethodHandle getMethodHandle(Method method) {
        return methodHandles.computeIfAbsent(method, queryMethod -> {
            // Use AnnotationUtils to find the @SrfQuery annotation on the method
            MethodHandles.Lookup lookup = MethodHandles.lookup();
            try {
                return lookup.unreflect(method);
            } catch (IllegalAccessException e) {
                throw new RuntimeException(e);
            }

        });
    }
}
