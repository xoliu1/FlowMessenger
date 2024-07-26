package com.xoliu.flowmessenger.invoke_strategy

import android.util.Log
import com.xoliu.aptprocessor.annotations.ExecutionMode
import com.xoliu.aptprocessor.annotations.Subscribe
import com.xoliu.aptprocessor.annotations.SubscribedMethod
import com.xoliu.aptprocessor.annotations.Subscription
import java.lang.reflect.Method

/***
 * 具体的实现订阅者方法实现类
 *
 * @param null
 * @return
 * @author xoliu
 * @create 24-7-25
 **/

class ReflectInvoke: MethodInvokeStrategy() {
    val TAG = "FlowMessenger - ReflectInvoke"
    /**
     * 获取这个订阅者类中所有的带{@link Subscribe}的方法
     *
     * @param subscriber 订阅者类，即通过register将this参数传过来的类，可
     *                   以是activity、service、fragment、thread等。
     */
    override fun getAllSubscribedMethods(subscriber: Any?): List<SubscribedMethod?>? {


        //记录订阅者方法参数
        val subscribedMethods: MutableList<SubscribedMethod> = ArrayList()
        val aClass: Class<*> = subscriber!!.javaClass

        //获取所有方法
        val declaredMethods = aClass.declaredMethods
        for (declaredMethod in declaredMethods) {
            if (declaredMethod.isAnnotationPresent(Subscribe::class.java)) {
                val parameterTypes = declaredMethod.parameterTypes
                require(parameterTypes.size <= 1) { "参数不能为空，且只能有一个参数" }
                val parameterType = parameterTypes[0]
                Log.d(TAG, "getAllSubscribedMethods: parameterType=" + parameterType.name)
                val annotation: Subscribe = declaredMethod.getAnnotation(Subscribe::class.java)
                val priority: Int = annotation.priority
                val executionMode: ExecutionMode = annotation.exeutionMode
                val subscribedMethod =
                    SubscribedMethod(declaredMethod, parameterType, executionMode, priority)
                Log.d(TAG, "getAllSubscribedMethods: subscribedMethod=$subscribedMethod")
                subscribedMethods.add(subscribedMethod)
            }
        }
        return subscribedMethods
    }

    override fun invokeMethod(subscription: Subscription?, event: Any?) {
        val subscriber = subscription!!.subscriber()
        val subscribedMethod = subscription.getTheSubscribedMethod()
        val method: Method? = subscribedMethod.method
        when (subscribedMethod.executionMode) {
            ExecutionMode.INSTANT -> {
                Log.d(TAG, "invokeMethod: ThreadMode=POSTING")
                invoke(method, subscriber, event)
            }

            ExecutionMode.MAIN_THREAD -> {
                Log.d(TAG, "invokeMethod: ThreadMode=MAIN")
                mainHandler!!.post { invoke(method, subscriber, event) }
            }

            ExecutionMode.BACKGROUND_THREAD -> {
                Log.d(TAG, "invokeMethod: ThreadMode=BACKGROUND")
                workHander!!.post { invoke(method, subscriber, event) }
            }
        }
    }

    fun invoke(method: Method?, subscriber: Any?, event: Any?){
        try {
            method!!.invoke(subscriber, event)
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }

}