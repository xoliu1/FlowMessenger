package com.xoliu.flowmessenger.invoke_strategy

import android.util.Log
import com.xoliu.aptprocessor.annotations.ExecutionMode
import com.xoliu.aptprocessor.annotations.MethodInvoker
import com.xoliu.aptprocessor.annotations.SubscribedMethod
import com.xoliu.aptprocessor.annotations.Subscription
import java.lang.reflect.InvocationTargetException

class AptAnnotationInvoke(methodInvoker: MethodInvoker) : MethodInvokeStrategy() {
    var methodInvoker: MethodInvoker = methodInvoker

    companion object {
        private const val TAG = "AptAnnotationInvoke"
    }

    override fun getAllSubscribedMethods(subscriber: Any?): List<SubscribedMethod?>? {
        return methodInvoker.getAllSubscribedMethods(subscriber!!.javaClass)

    }

    override fun invokeMethod(subscription: Subscription?, event: Any?) {
        val subscriber: Any = subscription!!.subscriber()
        val subscribedMethod: SubscribedMethod = subscription.getTheSubscribedMethod()
        val methodName: String? = subscribedMethod.methodName
        if (methodName != null) {
            when (subscribedMethod.executionMode) {
                ExecutionMode.INSTANT -> {
                    Log.d(TAG, "invokeMethod: ThreadMode=POSTING")
                    if (event != null) {
                        invoke(methodName, subscriber, event)
                    }
                }

                ExecutionMode.MAIN_THREAD -> {
                    Log.d(TAG, "invokeMethod: ThreadMode=MAIN")
                    mainHandler!!.post {
                        if (event != null) {
                            invoke(methodName, subscriber, event)
                        }
                    }
                }

                ExecutionMode.BACKGROUND_THREAD -> {
                    Log.d(TAG, "invokeMethod: ThreadMode=BACKGROUND")
                    workHander!!.post {
                        if (event != null) {
                            invoke(methodName, subscriber, event)
                        }
                    }
                }
            }
        }
    }

    private fun invoke(methodName: String, subscriber: Any, event: Any) {
        try {
            val declaredMethod = subscriber.javaClass.getDeclaredMethod(methodName, event.javaClass)
            declaredMethod.invoke(subscriber, event)
        } catch (e: IllegalAccessException) {
            e.printStackTrace()
        } catch (e: InvocationTargetException) {
            e.printStackTrace()
        } catch (e: NoSuchMethodException) {
            e.printStackTrace()
        }
    }
}