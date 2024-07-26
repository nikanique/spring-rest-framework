package io.github.nikanique.springrestframework.dto;


import io.github.nikanique.springrestframework.annotation.FieldValidation;
import io.github.nikanique.springrestframework.exceptions.ValidationException;

import java.lang.invoke.MethodHandle;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

public abstract class Dto {


    public final Map<String, String> validate(Boolean raiseValidationError) throws Throwable {  // Note: change method signature to throw Throwable
        Set<String> fields = DtoManager.getDtoByClassName(this.getClass()).entrySet().stream()
                .filter(entry -> entry.getValue().getValidation() != null)
                .map(Map.Entry::getKey)
                .collect(Collectors.toSet());
        return validate(fields, raiseValidationError);
    }

    public Map<String, String> validate(Set<String> fieldNames, Boolean raiseValidationError) throws Throwable {  // Note: change method signature to throw Throwable
        Map<String, String> validationErrors = new HashMap<>();
        Map<String, FieldMetadata> fieldMetadataMap = DtoManager.getDtoByClassName(this.getClass()).entrySet().stream()
                .filter(entry -> fieldNames.contains(entry.getKey()))
                .filter(entry -> entry.getValue().getValidation() != null)
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));

        for (Map.Entry<String, FieldMetadata> entry : fieldMetadataMap.entrySet()) {
            String fieldName = entry.getKey();
            FieldMetadata fieldMetadata = entry.getValue();
            MethodHandle getterMethodHandle = fieldMetadata.getGetterMethodHandle();
            Object value;

            if (getterMethodHandle != null) {
                value = getterMethodHandle.invoke(this);
            } else {
                throw new IllegalStateException("Getter method handle is not available for field: " + fieldName);
            }

            FieldValidation validation = fieldMetadata.getValidation();

            // Check for nullability
            if (!validation.nullable() && value == null) {
                validationErrors.put(fieldName, "The parameter cannot be null.");
            }

            if (value instanceof String) {
                validateStringField((String) value, fieldName, validation, validationErrors);
            } else if (value instanceof Number) {
                validateNumberField((Number) value, fieldName, validation, validationErrors);
            } else if (value instanceof java.util.Date) {
                validateDateField((java.util.Date) value, fieldName, validation, validationErrors);
            }
        }

        if (!validationErrors.isEmpty() && raiseValidationError) {
            throw new ValidationException(validationErrors);
        }

        return validationErrors;
    }

    private void validateStringField(String value, String fieldName, FieldValidation validation, Map<String, String> validationErrors) {
        if (!validation.blank() && value.trim().isEmpty()) {
            validationErrors.put(fieldName, "The parameter cannot be blank.");
        }

        if (value.length() < validation.minLength() || value.length() > validation.maxLength()) {
            validationErrors.put(fieldName, "String length must be between " +
                    validation.minLength() + " and " + validation.maxLength() + ".");
        }
    }

    private void validateNumberField(Number value, String fieldName, FieldValidation validation, Map<String, String> validationErrors) {
        double doubleValue = value.doubleValue();
        if (doubleValue < validation.minValue() || doubleValue > validation.maxValue()) {
            validationErrors.put(fieldName, "Value must be between " +
                    validation.minValue() + " and " + validation.maxValue() + ".");
        }
    }

    private void validateDateField(java.util.Date value, String fieldName, FieldValidation validation, Map<String, String> validationErrors) {
        SimpleDateFormat dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
        try {
            if (!validation.minDate().isEmpty()) {
                java.util.Date minDate = dateFormat.parse(validation.minDate());
                if (value.before(minDate)) {
                    validationErrors.put(fieldName, " date must be after " + validation.minDate() + ".");
                }
            }
            if (!validation.maxDate().isEmpty()) {
                java.util.Date maxDate = dateFormat.parse(validation.maxDate());
                if (value.after(maxDate)) {
                    validationErrors.put(fieldName, " date must be before " + validation.maxDate() + ".");
                }
            }
        } catch (ParseException e) {
            throw new IllegalArgumentException("Invalid date format in annotations for " + fieldName + ". Correct format is YYYY-MM-dd HH:mm:ss");
        }
    }

}
