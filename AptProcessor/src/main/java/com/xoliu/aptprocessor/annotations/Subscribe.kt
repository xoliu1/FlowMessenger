package com.xoliu.aptprocessor.annotations



@Target(AnnotationTarget.FUNCTION)
@Retention(AnnotationRetention.RUNTIME)
annotation class Subscribe(
    val exeutionMode: ExecutionMode = ExecutionMode.INSTANT,//默认为同步执行
    val priority: Int = 0//优先级默认为0
)
