package io.github.nikanique.springrestframework.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Expose {
    String name() default "not-provided";

    String format() default "not-provided";

    String methodName() default "not-provided";

    String source() default "not-provided";
}