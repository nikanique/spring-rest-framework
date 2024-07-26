package io.github.nikanique.springrestframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface FieldValidation {
    int maxLength() default Integer.MAX_VALUE;

    int minLength() default -1;

    boolean blank() default false;

    int minValue() default Integer.MIN_VALUE;

    int maxValue() default Integer.MAX_VALUE;

    boolean nullable() default true;

    String minDate() default "";

    String maxDate() default "";
}
