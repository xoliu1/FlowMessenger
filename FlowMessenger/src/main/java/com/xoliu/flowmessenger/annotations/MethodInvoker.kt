package com.xoliu.flowmessenger.annotations

interface MethodInvoker {
    fun getAllSubscribedMethods(subscriber: Any?): List<SubscribedMethod?>?

    fun invokeMethod(subscription: Subscription?, event: Any?)
}