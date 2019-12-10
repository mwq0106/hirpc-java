package com.hirpc.annotation;

import org.springframework.stereotype.Component;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * @author mwq0106
 * @date 2019/10/13
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
@Component
public @interface RpcService {
    /**
     * 服务类
     */
    Class<?> interfaceClass() default void.class;

    String interfaceName() default "";

    /**
     * 服务版本
     */
    String version() default "";
    /**
     * Service group, default value is empty string
     */
    String group() default "";
}
