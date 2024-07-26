package com.xoliu.aptprocessor.annotations

interface MethodInvoker {
    fun getAllSubscribedMethods(subscriber: Any?): List<SubscribedMethod?>?

    fun invokeMethod(subscription: Subscription?, event: Any?)
}