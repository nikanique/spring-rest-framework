package io.github.nikanique.springrestframework.common;

public enum FieldType {
    INTEGER(Integer.class.getSimpleName()),
    STRING(String.class.getSimpleName()),
    FLOAT(Float.class.getSimpleName()),
    DATE_TIME(java.util.Date.class.getSimpleName()),
    BOOLEAN(Boolean.class.getSimpleName()),
    DOUBLE(Double.class.getSimpleName()),
    TIMESTAMP(java.sql.Timestamp.class.getSimpleName()),
    LONG(Long.class.getSimpleName());

    private final String typeName;

    FieldType(String typeName) {
        this.typeName = typeName;
    }

    public static FieldType getByTypeName(String typeName) {
        for (FieldType fieldType : FieldType.values()) {
            if (fieldType.typeName.equals(typeName)) {
                return fieldType;
            }
        }
        return null; // Or throw an exception if you want to handle unknown types differently
    }


    public boolean isDateTime() {
        return this == DATE_TIME || this == TIMESTAMP;
    }
}
