package com.emiforcreatestock;

public @interface ForMoreParameter {
    Class<?>[] usingClass();

    String[] dataFrom() default "";

    String reason() default "";
}
