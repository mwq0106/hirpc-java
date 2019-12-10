package com.hirpc.annotation;

import com.hirpc.failmode.FailMode;
import com.hirpc.loadbalance.LoadBalance;

import java.lang.annotation.*;

/**
 * @author mwq0106
 * @date 2019/10/23
 */
@Documented
@Retention(RetentionPolicy.RUNTIME)
@Target({ElementType.FIELD, ElementType.METHOD, ElementType.ANNOTATION_TYPE})
public @interface RpcReference {
    /**
     * Interface class, default value is void.class
     */
    Class<?> interfaceClass() default void.class;

    /**
     * Service version, default value is empty string
     */
    String version() default "";

    /**
     * Service group, default value is empty string
     */
    String group() default "";

    LoadBalance loadBalance() default LoadBalance.ROUND_ROBIN;

    FailMode failMode() default FailMode.FAIL_FAST;
}
