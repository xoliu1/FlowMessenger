package com.xoliu.flowmessengers.MsgHub


import android.util.Log
import com.xoliu.aptprocessor.annotations.Subscription
import com.xoliu.flowmessenger.FlowMessengerBuilder
import com.xoliu.flowmessenger.invoke_strategy.AptAnnotationInvoke
import com.xoliu.flowmessenger.invoke_strategy.MethodInvokeStrategy
import com.xoliu.flowmessenger.invoke_strategy.ReflectInvoke
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


class FlowMessenger(builder: FlowMessengerBuilder? = DEFAULT_BUILDER) {
    private val TAG = "FlowMessenger"

    var invokeStrategy: MethodInvokeStrategy

    /**
     * 同一类型EventType类与所有注册方法的集合
     * key: EventType类
     * value: EventType类对应的Subscription
     */
    private val subscriptionsByEventType = mutableMapOf<Class<*>, CopyOnWriteArrayList<Subscription>>()

    /**
     * 所有类有注册FlowMessenger的类与其内部所有处理方法集合。
     * key: Subscriber
     * value: 该Subscriber中所有注册event的方法
     */
    private val typesBySubscriber = ConcurrentHashMap<Any, MutableList<Class<*>>>()

    companion object {
        var instance: FlowMessenger? = null
        private val DEFAULT_BUILDER = FlowMessengerBuilder()

        /**
         * 单例
         *
         * @return
         */
        fun getDefault(): FlowMessenger {
            if (instance == null) {
                synchronized(FlowMessenger::class.java) {
                    if (instance == null) {
                        instance = FlowMessenger()
                    }
                }
            }
            return instance!!
        }

        fun builder(): FlowMessengerBuilder {
            return FlowMessengerBuilder()
        }
    }

    init {
        invokeStrategy = if (builder?.methodInvoker != null) {
            // 注解处理器获取订阅者方法和调用
            AptAnnotationInvoke(builder.methodInvoker!!)
        } else {
            // 反射获取订阅者方法和反射调用方法
            ReflectInvoke()
        }
    }

    /**
     * 注册subscriber到FlowMessenger，并获取其所有加了{@link Subscribe} 的方法，并放入集合中
     *
     * @param subscriber 订阅者类，即通过register将this参数传过来的类，可以是activity、service、fragment、thread等。
     */
    fun register(subscriber: Any) {
        // 检查订阅者是否已经注册
        if (typesBySubscriber.containsKey(subscriber)) {
            Log.w(TAG, "Subscriber is already registered.")
            return
        }

        val allSubscribedMethods = invokeStrategy.getAllSubscribedMethods(subscriber)
        if (allSubscribedMethods.isNullOrEmpty()) {
            Log.e(TAG, "register: there is no method found!")
            return
        }

        for (subscribedMethod in allSubscribedMethods) {
            val eventType = subscribedMethod!!.eventType
            if (eventType!=null){
                val subscriptions = subscriptionsByEventType.getOrPut(eventType) { CopyOnWriteArrayList() }
                // 以下为priority逻辑，在此处排序添加
                // 获取订阅方法的优先级
                val priority = subscribedMethod.priority
                // 创建新的 Subscription 实例
                val newSubscription = Subscription(subscriber, subscribedMethod, priority)
                // 将新订阅插入到适当的位置以保持优先级顺序
                val index = subscriptions.indexOfFirst { newSubscription.compareTo(it) > 0 }
                if (index >= 0) {
                    subscriptions.add(index, newSubscription)
                } else {
                    subscriptions.add(newSubscription)
                }
                // 至此完成优先级的排序

                printSubscriptionsByEventType(subscriptionsByEventType)
                // 获取这个订阅者类中记录的所有的eventType类型
                val eventTypesInSubscriber = typesBySubscriber.getOrPut(subscriber) { mutableListOf() }
                eventTypesInSubscriber.add(eventType)
            }else{
                Log.d(TAG, "register: eventType为空！")
            }

        }
        printTypesBySubscriber(typesBySubscriber, subscriber)
    }

    private fun printSubscriptionsByEventType(subscriptionsByEventType: Map<Class<*>, CopyOnWriteArrayList<Subscription>>) {
        for ((eventType, subscriptions) in subscriptionsByEventType) {
            for (subscription in subscriptions) {
                Log.d(TAG, "printSubscriptionsByEventType: eventType=${eventType.name} subscription=$subscription")
            }
        }
    }

    private fun printTypesBySubscriber(list: Map<Any, List<Class<*>>>, subscriber: Any) {
        val classes = list[subscriber]
        if (!classes.isNullOrEmpty()) {
            for (aClass in classes) {
                Log.d(TAG, "register: typesBySubscriber=${aClass.name}")
            }
        }
    }

    /**
     * 发送event消息到订阅者 处理方法
     *
     * @param event
     */
    fun post(event: Any) {
        if (subscriptionsByEventType.isEmpty()) {
            Log.e(TAG, "post: no any eventbus registered named ${event::class.java}")
            return
        }

        Log.d(TAG, "event.getClass()=${event::class.java.name}")
        val subscriptions = subscriptionsByEventType[event::class.java]
        subscriptions?.forEach { subscription ->
            invokeStrategy.invokeMethod(subscription, event)
        }
    }

    /**
     * 解注册FlowMessenger
     *
     * @param subscriber
     */
    fun unregister(subscriber: Any) {
        val subscribedEventTypes = typesBySubscriber[subscriber]
        subscribedEventTypes?.forEach { eventType ->
            unsubscribe(subscriber, eventType)
        }
        typesBySubscriber.remove(subscriber)
    }

    private fun unsubscribe(subscriber: Any, eventType: Class<*>) {
        val subscriptions = subscriptionsByEventType[eventType]
        subscriptions?.removeIf { it.subscriber == subscriber }
        if (subscriptions.isNullOrEmpty()) {
            subscriptionsByEventType.remove(eventType)
        }
    }
}
