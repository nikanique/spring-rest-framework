package io.github.nikanique.springrestframework.annotation;

import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;

@Retention(RetentionPolicy.RUNTIME)
public @interface ReferencedModel {
    String model();

    String referencingField();
}
