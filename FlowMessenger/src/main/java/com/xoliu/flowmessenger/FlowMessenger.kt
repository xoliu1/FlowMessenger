package com.xoliu.flowmessengers.MsgHub


import android.util.Log
import com.xoliu.aptprocessor.annotations.Subscribe
import com.xoliu.aptprocessor.annotations.Subscription
import com.xoliu.flowmessenger.FlowMessengerBuilder
import com.xoliu.flowmessenger.invoke_strategy.AptAnnotationInvoke
import com.xoliu.flowmessenger.invoke_strategy.MethodInvokeStrategy
import com.xoliu.flowmessenger.invoke_strategy.ReflectInvoke
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.CopyOnWriteArrayList


class FlowMessenger private constructor(builder: FlowMessengerBuilder) {
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
    // 存储粘性事件
    private val stickyEvents = ConcurrentHashMap<Class<*>, Any>()


    init {
        invokeStrategy = builder.methodInvoker?.let { AptAnnotationInvoke(it) } ?: ReflectInvoke()
    }

    companion object {
        @Volatile
        private var instance: FlowMessenger? = null

        /**
         * 获取单例实例
         *
         * @return FlowMessenger单例
         */
        fun getInstance(builder: FlowMessengerBuilder = FlowMessengerBuilder()): FlowMessenger {
            return instance ?: synchronized(this) {
                instance ?: FlowMessenger(builder).also { instance = it }
            }
        }

        /**
         * 返回一个新的 FlowMessengerBuilder
         */
        fun builder(): FlowMessengerBuilder {
            return FlowMessengerBuilder()
        }
    }
    /**
     * 注册subscriber到FlowMessenger，并获取其所有加了{@link Subscribe} 的方法，并放入集合中
     *
     * @param subscriber 订阅者类，即通过register将this参数传过来的类，可以是activity、service、fragment、thread等。
     */
    fun register(subscriber: Any) {
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
            if (eventType != null) {

                val subscriptions = subscriptionsByEventType.getOrPut(eventType) { CopyOnWriteArrayList() }
                val priority = subscribedMethod.priority
                val newSubscription = Subscription(subscriber, subscribedMethod, priority)
                if (subscribedMethod.sticky){
                    stickyEvents[eventType] = newSubscription
                }
                //Log.d("TAG123", "$stickyEvents")
                val index = subscriptions.indexOfFirst { newSubscription.compareTo(it) > 0 }
                if (index >= 0) {
                    subscriptions.add(index, newSubscription)
                } else {
                    subscriptions.add(newSubscription)
                }
                val eventTypesInSubscriber = typesBySubscriber.getOrPut(subscriber) { mutableListOf() }
                eventTypesInSubscriber.add(eventType)
                // 处理粘性事件
                stickyEvents[eventType]?.let {
                    val subscriptions = subscriptionsByEventType[eventType]
                    //Log.d("TAG12345", "$subscriptions")
                    subscriptions?.forEach { subscription ->
                        invokeStrategy.invokeMethod(subscription, map[eventType])
                    }
                }
            } else {
                Log.d(TAG, "register: eventType为空！")
            }

        }
        printSubscriptionsByEventType(subscriptionsByEventType)
        //printTypesBySubscriber(typesBySubscriber, subscriber)
    }


    private fun printSubscriptionsByEventType(subscriptionsByEventType: Map<Class<*>, CopyOnWriteArrayList<Subscription>>) {
        for ((eventType, subscriptions) in subscriptionsByEventType) {
            for (subscription in subscriptions) {
                Log.d(TAG, "printSubscriptionsByEventType: eventType=${eventType.name} \n" +
                        "subscription=$subscription")
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
     * 发送event消息到订阅者处理方法
     *
     * @param event
     */
    val map:MutableMap<Class<*>, Any> = HashMap()
    fun emitEvent(event: Any) {
        if (subscriptionsByEventType.isEmpty()) {
            Log.e(TAG, "post: no any eventbus registered named ${event::class.java}")
            return
        }
        val eventType = event::class.java
       map[eventType] = event
        //Log.d("TAG1234", "$eventType")//class com.xoliu.flowmessengers.Event.WorkEvent
        val subscriptions = subscriptionsByEventType[eventType]
        //Log.d("TAG12345", "$subscriptions")
        subscriptions?.forEach { subscription ->
            //Log.d("TAG1234", "$event")//com.xoliu.flowmessengers.Event.WorkEvent@ff58711
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
            stickyEvents.remove(eventType)
        }
    }

    /**
     * 发送粘性事件（弃用）
     *
     * @param event
     */
    fun postSticky(event: Any) {
        stickyEvents[event::class.java] = event
        emitEvent(event)
    }
}
