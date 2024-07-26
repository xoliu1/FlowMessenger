package com.xoliu.aptprocessor.annotations

import java.lang.reflect.Method

/***
 * SubscribedMethod 封装了订阅者方法的相关信息，
 * 包括所在类、参数类型、线程模式、优先级和方法名。
 * 在事件发布时，FlowMessenger 将根据这些信息找到并调用订阅者方法。
 * @param null
 * @return
 * @author xoliu
 * @create 24-7-25
 **/


data class SubscribedMethod(
    val method: Method? = null,
    val eventType: Class<*>?,
    val executionMode: ExecutionMode,
    val priority: Int,
    val methodName: String? = null,
    val subscriberClass: Class<*>? = null
) {

    constructor(
        method: Method,
        eventType: Class<*>,
        executionMode: ExecutionMode,
        priority: Int
    ) : this(method, eventType, executionMode, priority, null, null)

    constructor(
        subscriberClass: Class<*>,
        eventType: Class<*>,
        executionMode: ExecutionMode,
        priority: Int,
        methodName: String
    ) : this(null, eventType, executionMode, priority, methodName, subscriberClass)

    override fun toString(): String {
        return "SubscribedMethod(method=$method, eventType=$eventType, threadMode=$executionMode, priority=$priority)"
    }
}
