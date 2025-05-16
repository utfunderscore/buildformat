package org.readutf.buildformat.common.format.requirements;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Retention(RetentionPolicy.RUNTIME)
public @interface Requirement {
    String name() default "";

    String startsWith() default "";

    String endsWith() default "";

    String regex() default "";

    int minimum() default 1;
}