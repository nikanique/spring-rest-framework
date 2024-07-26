package io.github.nikanique.springrestframework.filter;


import io.github.nikanique.springrestframework.common.FieldType;

import java.util.Set;
import java.util.TreeSet;

public class FilterSet {
    private final Set<Filter> filters;

    public FilterSet(Set<Filter> filters) {
        this.filters = filters;
    }


    public FilterSet() {
        this.filters = new TreeSet<>();
    }

    public static FilterSetBuilder builder() {
        return new FilterSetBuilder();
    }

    public Set<Filter> getFilters() {
        return filters;
    }

    public void add(Filter filter) {
        filters.add(filter);
    }


    public static class FilterSetBuilder {

        private final FilterSet filterSet;

        private FilterSetBuilder() {
            filterSet = new FilterSet();
        }

        public FilterSetBuilder addFilter(String name, FilterOperation operation, FieldType fieldType) {
            Filter filter = new Filter(name, operation, fieldType);
            filterSet.add(filter);
            return this;
        }

        public FilterSetBuilder addFilter(String name, FilterOperation operation, FieldType fieldType, String helpText) {
            Filter filter = new Filter(name, operation, fieldType, helpText);
            filterSet.add(filter);
            return this;
        }

        public FilterSetBuilder addFilter(String name, String modelFieldName, FilterOperation operation, FieldType fieldType) {
            Filter filter = new Filter(name, modelFieldName, operation, fieldType);
            filterSet.add(filter);
            return this;
        }

        public FilterSetBuilder addFilter(String name, String modelFieldName, FilterOperation operation, FieldType fieldType, String helpText) {
            Filter filter = new Filter(name, modelFieldName, operation, fieldType, helpText);
            filterSet.add(filter);
            return this;
        }

        public FilterSet build() {
            return filterSet;
        }
    }
}
