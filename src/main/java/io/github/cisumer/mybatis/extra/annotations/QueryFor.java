package io.github.cisumer.mybatis.extra.annotations;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

@Target(ElementType.FIELD)
@Documented
@Retention(RetentionPolicy.RUNTIME)
public @interface QueryFor {
	String select() default "";
	String resultMap() default "";
	String columnPrefix() default "";
	String column() default "";
	String property();
}
